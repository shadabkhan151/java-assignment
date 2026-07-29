# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
 There are three styles in the same monolith:
  - Store      -> Panache active record (static Store.findById/listAll) called straight from
                  the JAX-RS resource.
  - Product    -> Panache repository injected into the resource, entity still exposed as the
                  API payload.
  - Warehouse  -> ports & adapters: a domain model, a WarehouseStore port, a DbWarehouse JPA
                  entity and a repository adapter, with use cases holding the rules.
                  
I would not force all three into one style, but I would converge on a rule of thumb, because
the cost here is not "inconsistency" as an aesthetic complaint - it is that a reader cannot
predict where a rule lives, and that two of the three styles make the rules untestable without
a database.

What I would keep:

  - The Warehouse style, for anything that carries real domain rules. It is the only one of the
    three where I can unit test "a warehouse cannot exceed the location capacity" in
    milliseconds, with no container and no Postgres (see CreateWarehouseUseCaseTest). The
    separate DbWarehouse/Warehouse pair also pays for itself the moment persistence and the
    domain disagree - soft delete via archivedAt is a persistence concern that the domain reads
    as "active or not".
  - Active record for genuinely CRUD-only aggregates. Panache is a productivity feature and
    paying the mapping tax on an entity with no behaviour is a bad trade.

What I would change, roughly in order of value:

  1. No persistence calls inside a JAX-RS resource. Even for simple CRUD, the resource should
     delegate to a repository or a service. It is what makes the transaction boundary and the
     "what happens around the write" question (see the legacy sync in task 2) explicit rather
     than incidental.
  2. Stop returning JPA entities as API payloads (Store, Product). It couples the public
     contract to the schema, leaks columns by accident, and makes it impossible to evolve one
     without the other. DTOs, like the fulfillment endpoints I added.
  3. Consolidate error handling. Two @Provider ExceptionMapper<Exception> classes are
     registered (one nested in ProductResource, one in StoreResource) - which one wins is not
     something I want to reason about. One global mapper per meaningful exception type, which
     is the approach I used for BusinessRuleViolationException / ResourceNotFoundException.
  4. Make transaction boundaries deliberate and one level deep. I put @Transactional on the
     REST adapter for warehouses so that "archive + create" during a replacement is atomic; the
     repository methods also declare it so they are safe standalone, and they simply join.
  5. Model gaps worth fixing at the same time: `int` for quantities makes "not provided" and
     "zero" indistinguishable (visible in the PATCH logic), and uniqueness of an active
     business unit code is only enforced in application code - two concurrent creates can still
     both pass. That needs a partial unique index (unique on business_unit_code where
     archived_at is null) - the database is the only place that check is actually safe.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
  Contract first (generate from the yaml)

  + The contract is a reviewable artifact. A breaking change shows up as a diff in a pull
    request, before anyone writes code, and consumers can be part of that conversation.
  + Documentation cannot drift, because it is the source.
  + The same file generates server stubs, client SDKs and mocks, so a consuming team can start
    integrating before the implementation exists.
  + It forces API design decisions up front instead of letting them fall out of the entity
    shape - exactly the mistake visible on the Store endpoints, where the JPA entity *is* the
    contract.

  - You write against generated code, so you lose some control. Concretely, in this assignment:
    the generated interface returns the bean, not Response, so the documented 201 needs a
    framework-specific annotation (@ResponseStatus) instead of being obvious.
  - A spec that is incomplete gives a false sense of safety. This one has no required fields,
    no error schema and no 409, so validation and error contracts live in the code anyway.
  - Build/IDE friction (regeneration, marking target/.../jaxrs as generated sources - the
    README already warns about it) and a dependency on a generator's opinions and version.

Code first

  + Fast, idiomatic, full access to framework features, nothing between you and the endpoint.
  + Fine when the API has exactly one consumer that ships with it.

  - Without discipline there is no contract at all, which is the current state for Product and
    Store: nothing describes them, nothing detects a breaking change, nothing is generated for
    consumers.
  - The contract tends to become "whatever the code happens to serialize".

My choice

  Contract first for anything crossing a team or company boundary - which is precisely the
  Warehouse API and, in this domain, anything the legacy or financial systems consume. The
  cost is real but it is paid once, and it buys reviewable evolution of the contract.

  For internal-only endpoints I am happy with code first, on one condition: generate the spec
  from the code (quarkus-smallrye-openapi) and publish it, then diff it in CI against the
  committed version so a breaking change is a failing build rather than a support ticket. That
  gets most of the benefit without the generator in the middle.

  Whichever direction, I would invest in the spec itself before more endpoints: required
  fields, an error schema, the 409 for a taken business unit code, and examples. Right now the
  yaml types the path parameter as `id` while the Warehouse schema carries both `id` and
  `businessUnitCode` - ambiguous enough that I made the handler accept either, and that
  ambiguity is exactly the kind of thing a contract should have settled.

```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority comes from risk, not from layers: what is expensive if it is wrong, and what is hard
to reason about by reading the code. In this domain that is stock and capacity arithmetic, the
replacement flow (a business unit temporarily has no active warehouse), and anything that
crosses the transaction boundary.

Where I put the effort, in order:

1. Use case unit tests - the bulk of the assertions.
   Every rule (business unit code taken, unknown location, no free slot at the location,
   location capacity, stock <= capacity, capacity accommodation and stock matching on
   replacement) is tested against an in-memory WarehouseStore, with no container and no
   database. Milliseconds per test, so nobody is tempted to skip them, and they fail with a
   message about the rule rather than about SQL. The fake deliberately returns copies and
   copies state back on update(), so a use case that forgets to persist fails the test instead
   of quietly passing through a shared reference.

2. A thin layer of endpoint tests against the real stack (@QuarkusTest + Dev Services).
   Not to re-test the rules, but to test what unit tests structurally cannot: wiring,
   serialization, status codes, exception mapping, and above all transaction behaviour. Two of
   them earn their keep on their own:
     - the failed replacement, which asserts that the previously archived warehouse is still
       active afterwards, i.e. that the rollback works;
     - StoreLegacySyncTest, which asserts the legacy system is notified on commit and *not*
       notified when the insert fails at flush time. That was the actual requirement of task 2
       and it is invisible to a unit test.

3. A couple of integration tests on the packaged application (@QuarkusIntegrationTest) as a
   smoke test that the thing boots and serves - deliberately shallow.

What I would deliberately not test: generated code, getters, and framework behaviour.

Keeping it effective over time:

  - Test behaviour through the ports, not implementation. My use case tests never mention JPA,
    so the day the repository changes they still pass - which is the whole point.
  - Coverage as a signal, not a target. A percentage gate on the whole project gets gamed with
    tests of trivia.
  - Every production bug gets a failing test first, then the fix. That is how the suite ends
    up covering what actually breaks instead of what was easy to write.
  - Keep them fast and deterministic, otherwise they get ignored. The @QuarkusTest classes here
    share one database, so they are ordered and use their own fixtures rather than relying on
    import.sql rows another test may have deleted; if the suite grew, I would isolate state per
    class rather than sequence it harder.
  - Two gaps I would close next, and I would rather name them than pretend they are covered:
    a concurrency test for two simultaneous creations of the same business unit code (which
    will fail until the partial unique index exists), and a contract test against the legacy
    store integration so its shape is pinned down.

```