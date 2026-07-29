# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
```txt
Questions I would ask before scoping anything

  - What decision does this number support? "Should we close this site", "how do we price
    fulfilment for this client" and "did we hit budget" need very different accuracy and
    granularity. The first two justify a lot of engineering; the third often does not.
  - Who is the audience - finance or operations? Finance wants numbers that reconcile to the
    general ledger. Operations wants a number they can influence this week. Those are usually
    two different views over the same data, not one compromise.
  - What granularity is actually actionable: per business unit, per store, per SKU, per order?
    Cost per unit of *something* is what makes sites comparable, so what is the denominator?
  - Are we the system of record for cost, or a view over the ERP? I would fight hard for the
    second: a second bookkeeping system nobody reconciles is worse than no system.
  - Which allocation keys does finance already accept? Allocation is a negotiation before it is
    an algorithm.
  - How are corrections handled - restate history or post an adjustment? That single answer
    drives the whole data model.

Considerations

  - The hard part is not the direct costs (labour hours, transport per shipment); it is the
    shared and indirect ones. Every allocation key is an opinion, so the model has to be
    explainable, stable, and consistent between what you budget and what you report - otherwise
    the variance you show is an artifact of the method and site managers stop trusting it.
  - I would separate two layers: an immutable ledger of raw cost events (what happened, where,
    when, in which currency, with which source document) and a derived allocation layer that is
    recomputable. Allocation rules become versioned, effective-dated data rather than code, so
    when the rule changes in April you can still explain March, and you can re-run history under
    a new rule without destroying the old answer.
  - Timing kills accuracy more often than the algorithm does: invoices arrive weeks late, so a
    month that looks great in week one deteriorates. Accruals, and a clear "provisional vs
    final" flag on every figure, matter more than another decimal place.
  - Master data alignment is the unglamorous risk: our Warehouse must map to a cost centre, our
    Store to whatever finance calls it. In this code base the business unit code is that join
    key - and it is deliberately reused across replacements, which means any cost query keyed
    only on the code silently merges two different physical sites. Cost records should reference
    the concrete warehouse instance and carry the period, with the business unit code as the
    grouping key on top.
  - Perfect accuracy is not the goal; consistent, explainable and timely is. A number everyone
    can trace back to a driver beats a more precise one nobody can defend.
```

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
```txt
Questions

  - What is the current cost-per-unit baseline, and does anyone trust it? Without scenario 1
    working, every "saving" below is unprovable.
  - Which costs are actually controllable in the horizon we care about? Long leases, 3PL
    minimums and collective labour agreements make a lot of the cost base fixed for a year.
  - Which service levels are non-negotiable, and what do we already know about the revenue cost
    of missing them? "Without compromising service quality" needs a number, not an adjective.
  - What is the payback horizon and the appetite for capex? Automation and network changes are
    a different conversation from scheduling and packaging.

Where the money usually is

  - Labour, which is typically the largest controllable line: schedule against a volume forecast
    instead of a fixed roster, reduce travel distance through slotting and pick paths, batch
    picking.
  - Network and sourcing decisions: which warehouse fulfils which product for which store. This
    is exactly the association feature in the bonus task, and it is a cost lever, not just a
    data model - splitting a store order across warehouses adds a shipment. The "max 2
    warehouses per product per store" rule is a deliberate trade between resilience and
    transport cost, and it is worth being explicit that it *is* a trade.
  - Capacity utilisation: locations have a maximum capacity and a maximum number of warehouses;
    half-used capacity is paid for whether or not it is used.
  - Transport consolidation, packaging and dimensional weight, returns handling, energy.

How I would identify and prioritise

  - Start with a driver tree from the unit cost, then benchmark the same metric across sites.
    Site-to-site variance is the cheapest hypothesis generator there is: if one warehouse picks
    at 30% lower cost per line, the question answers itself.
  - Pareto the cost base, then score candidates on impact x confidence x effort, with an explicit
    service-risk flag. Fast, reversible, low-risk items first - they also buy credibility for
    the expensive ones.
  - Beware local optimisation. Cutting warehouse cost by consolidating sites moves the money to
    transport. Anything I propose gets evaluated on total cost to serve, not on one line.

How I would implement

  - As experiments: pilot on one or two sites, keep comparable sites as control, agree the
    success metric and the service guardrails *before* starting, and measure with the same
    allocation model on both sides so the comparison means something.
  - Track service KPIs on the same dashboard as the savings. The failure mode of cost programmes
    is a saving that shows up this quarter and a churn cost that shows up next year.
  - Expect a distribution of outcomes: a few percent from scheduling and slotting is realistic
    and quick; network redesign is larger and slower. I would rather commit to a range and the
    measurement method than to a single number up front.
```

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
```txt
Questions

  - For each field, which system is the source of truth? My default: the ERP owns financial
    truth, we own operational and business-unit master data. Ambiguity here is what produces
    two numbers in two systems and a meeting to decide which is right.
  - What does "real-time" mean to the people asking? Sub-second, or "I do not want to wait for
    the month-end close"? In my experience it is nearly always the second, and intra-day
    incremental sync is an order of magnitude cheaper than genuine streaming.
  - What can the financial system actually do - files on a schedule, a batch API, events? That
    usually constrains the design more than our side does.
  - Volume and shape: thousands of cost events a day, or millions?
  - How are period locks handled? Posting into a closed period has to fail loudly, not silently
    land in the next one.
  - What are the audit and controls requirements? If this feeds statutory reporting, traceability
    from a reported number back to the source document is a requirement, not a nice-to-have.

Benefits worth stating in business terms

  - One set of numbers, so the discussion is about the decision rather than about whose export
    is right; faster close; earlier warning on overruns instead of a surprise six weeks later;
    an auditable trail; and operational context (volumes, capacity) sitting next to financial
    values, which is what makes cost per unit possible at all.

How I would make it robust

  - Event driven where possible, with an explicit contract and versioning, plus a scheduled
    reconciliation as the safety net. Every integration drifts; the only question is whether you
    detect it or a controller does.
  - Idempotency keys and at-least-once delivery. Financial data cannot tolerate a duplicate
    posting caused by a retry, and exactly-once does not exist in practice.
  - A transactional outbox rather than "call the other system inside the transaction". This is
    the same problem as task 2 in the code assignment: I moved the legacy call to fire after the
    commit, which fixes the "we told them about a change we rolled back" failure. The remaining
    gap - crash between commit and call - is exactly what an outbox closes, and I would use one
    here because a lost cost event is a wrong ledger.
  - An explicit, owned mapping between our business unit codes and their cost centres, with
    effective dates. Given that a business unit code is reused when a warehouse is replaced,
    the mapping has to be time-aware or the two sites' costs merge.
  - Reconciliation dashboards and alerting on drift, dead-letter handling with a human path, and
    a documented replay procedure. "Seamless" in practice means the failures are visible and
    recoverable, not that they never happen.
```

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
```txt
Why it matters here specifically

  - Fulfilment cost is mostly capacity that has to be committed in advance: people hired and
    trained, space leased, transport contracted. A forecast that is two weeks late is not a
    reporting problem, it is an overtime bill or a missed peak. Budgeting is also the mechanism
    that turns "we should open/replace a warehouse" into a decision with a number attached.

Questions

  - What is the planning cycle, who owns the numbers, and who signs off? A forecasting system
    without an owner produces reports nobody acts on.
  - Horizon and granularity: weekly by site for twelve weeks is a different system from monthly
    by business unit for three years.
  - Which drivers do we have clean history for? Volume is usually the honest one; everything
    else tends to be modelled off it.
  - Annual budget, rolling forecast, or both - and do they have to reconcile top-down and
    bottom-up?
  - How much accuracy is actionable? If the labour plan can only flex in half-day blocks, a
    forecast three times more precise than that buys nothing.

Design considerations

  - Driver based, not "last year plus five percent": forecast the volume, then apply unit costs
    per driver. It makes the forecast explainable and lets you attribute a variance to volume
    versus rate versus mix - which is the whole point of the exercise.
  - Same cost model as the actuals. If budget and actual are cut differently, the variance is
    noise and people stop looking.
  - Assumptions and scenarios as first-class, versioned data: every number should answer "which
    version, whose assumptions, when". Business planning is a conversation between versions.
  - Seasonality, promotions and the commercial calendar are the dominant signal in retail
    fulfilment - peak is where budgets are actually won or lost. That calendar has to be an
    input, not something a model has to infer.
  - Track forecast accuracy (error and bias, per site and per horizon) as a metric of its own,
    and review it. A forecast nobody scores does not improve.
  - Start with a statistical baseline plus structured human override, and capture the overrides.
    Planners hold real information the model does not have; capturing it beats fighting it, and
    it is also the training data for anything more sophisticated later.
  - Handle sites without history explicitly: a new or replaced warehouse cannot be forecast from
    its own past. It needs a ramp-up curve and a comparable site as reference - which is another
    reason the predecessor's cost history has to survive a replacement (scenario 5).
  - Transparency over sophistication. A model the site manager can argue with gets used; a black
    box gets a spreadsheet built next to it.
```

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
```txt
Questions

  - Is there an overlap period where both warehouses are operational? That is where the cost
    control problem actually lives, and the current model - archive the old one at the instant
    the new one is created - does not express an overlap.
  - How are transition costs classified: dual running, moving stock, ramp-up inefficiency,
    decommissioning, write-offs, possibly capex on the new site? Who approves them, and are
    they inside the business unit's budget or a separate project budget? My strong preference is
    separate, otherwise the new site's steady-state performance is unreadable for a year.
  - What is the expected ramp-up curve, and at what point do we start holding the new site to
    the target unit cost rather than to a ramp-up allowance?
  - Do downstream systems key on the business unit code alone? If so, they will silently
    concatenate the two sites' histories - which is sometimes exactly what you want (business
    unit trend) and sometimes very wrong (site performance).
  - What happens to the old site's open commitments and contracts?

Why the history has to survive

  - The business unit code is the continuity key: it is what lets you say "this area costs us X
    per order and it has moved this way over three years". Overwriting it would delete the only
    baseline available for judging the replacement - you would have no way to answer the
    question the investment was justified on.
  - The new warehouse's budget should be derived from the predecessor's actuals plus the
    expected improvement. No history, no defensible budget - and then "within budget" only means
    "within a number someone guessed".
  - Variance analysis needs a like-for-like comparison, adjusted for volume and mix. That is
    only possible if the old cost records still exist at the same granularity.
  - Audit and asset accounting need it independently of any of the above.

How that maps to the implementation

  - The model already supports it: archiving is a soft delete (archivedAt is stamped, the row
    stays), and the replacement creates a new row with the same business unit code. So a
    business unit is really a timeline of warehouse instances, and you can slice cost either by
    instance (site performance, ramp-up) or by business unit code across time (area trend).
    Cost records should therefore reference the warehouse instance, not just the code.
  - I implemented the replacement as archive-then-create inside a single transaction, so it
    cannot half-happen: a business unit is never left archived with no successor, and a failed
    validation leaves the original untouched (there is an endpoint test for exactly that).
  - What I would add for cost control specifically: an explicit overlap/effective-dating model
    instead of an instantaneous switch, an audit record of who replaced what and why, and a
    transition cost category with its own budget line - so that the day someone asks "is the new
    warehouse within budget?", the answer is not distorted by one-off costs that were always
    expected.
```

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
