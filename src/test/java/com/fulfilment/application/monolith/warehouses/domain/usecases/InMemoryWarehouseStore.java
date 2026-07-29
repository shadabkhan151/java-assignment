package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory test double for {@link WarehouseStore}.
 *
 * <p>It hands out <b>copies</b> and copies the state back on {@code update}, exactly like the JPA
 * adapter does when it maps rows to domain objects. That way a use case that forgets to call
 * {@code update()} fails the test instead of silently "working" through a shared reference.
 */
public class InMemoryWarehouseStore implements WarehouseStore {

  private final List<Warehouse> warehouses = new ArrayList<>();
  private final AtomicLong sequence = new AtomicLong();

  public InMemoryWarehouseStore(Warehouse... initialWarehouses) {
    for (Warehouse warehouse : initialWarehouses) {
      create(warehouse);
    }
  }

  @Override
  public List<Warehouse> getAll() {
    return warehouses.stream().filter(w -> !w.isArchived()).map(InMemoryWarehouseStore::copyOf).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    Warehouse stored = copyOf(warehouse);
    stored.id = sequence.incrementAndGet();
    if (stored.createdAt == null) {
      stored.createdAt = LocalDateTime.now();
    }
    warehouses.add(stored);

    warehouse.id = stored.id;
    warehouse.createdAt = stored.createdAt;
  }

  @Override
  public void update(Warehouse warehouse) {
    Warehouse stored = findStored(warehouse);
    stored.businessUnitCode = warehouse.businessUnitCode;
    stored.location = warehouse.location;
    stored.capacity = warehouse.capacity;
    stored.stock = warehouse.stock;
    stored.createdAt = warehouse.createdAt;
    stored.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    warehouses.remove(findStored(warehouse));
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return warehouses.stream()
        .filter(w -> !w.isArchived())
        .filter(w -> Objects.equals(w.businessUnitCode, buCode))
        .findFirst()
        .map(InMemoryWarehouseStore::copyOf)
        .orElse(null);
  }

  @Override
  public Warehouse findActiveById(Long id) {
    return warehouses.stream()
        .filter(w -> !w.isArchived())
        .filter(w -> Objects.equals(w.id, id))
        .findFirst()
        .map(InMemoryWarehouseStore::copyOf)
        .orElse(null);
  }

  /** Everything ever stored, archived rows included — used to assert on history. */
  public List<Warehouse> everything() {
    return List.copyOf(warehouses);
  }

  private Warehouse findStored(Warehouse warehouse) {
    return warehouses.stream()
        .filter(w -> Objects.equals(w.id, warehouse.id))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Warehouse " + warehouse.businessUnitCode + " does not exist."));
  }

  private static Warehouse copyOf(Warehouse source) {
    Warehouse copy = new Warehouse();
    copy.id = source.id;
    copy.businessUnitCode = source.businessUnitCode;
    copy.location = source.location;
    copy.capacity = source.capacity;
    copy.stock = source.stock;
    copy.createdAt = source.createdAt;
    copy.archivedAt = source.archivedAt;
    return copy;
  }
}
