package com.fulfilment.application.monolith.stores;

/**
 * Immutable snapshot of a {@link Store} change, fired as a CDI event by {@link StoreResource}.
 *
 * <p>It carries a <b>copy</b> of the data on purpose: the observer runs after the transaction has
 * been committed, when the JPA entity is already detached, so passing the managed entity around
 * would be a source of lazy-loading/stale-state surprises.
 */
public class StoreSyncEvent {

  public enum Operation {
    CREATED,
    UPDATED
  }

  private final Operation operation;
  private final Long id;
  private final String name;
  private final int quantityProductsInStock;

  private StoreSyncEvent(Operation operation, Long id, String name, int quantityProductsInStock) {
    this.operation = operation;
    this.id = id;
    this.name = name;
    this.quantityProductsInStock = quantityProductsInStock;
  }

  public static StoreSyncEvent created(Store store) {
    return new StoreSyncEvent(
        Operation.CREATED, store.id, store.name, store.quantityProductsInStock);
  }

  public static StoreSyncEvent updated(Store store) {
    return new StoreSyncEvent(
        Operation.UPDATED, store.id, store.name, store.quantityProductsInStock);
  }

  public Operation getOperation() {
    return operation;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getQuantityProductsInStock() {
    return quantityProductsInStock;
  }

  /** Rebuilds a detached {@link Store} to hand over to the legacy gateway. */
  public Store toDetachedStore() {
    Store store = new Store(name);
    store.id = id;
    store.quantityProductsInStock = quantityProductsInStock;
    return store;
  }
}
