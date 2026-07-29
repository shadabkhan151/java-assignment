package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Panache based adapter for the {@link WarehouseStore} port.
 *
 * <p>All queries filter out archived rows: archiving is a soft delete that keeps the history of a
 * business unit code, so "the warehouse MWH.001" always means "the active one".
 *
 * <p>The write methods are {@code @Transactional} (REQUIRED) so the repository is safe to use on
 * its own, but in practice they join the transaction opened by the REST adapter, which is what
 * makes "archive + create" of a replacement atomic.
 */
@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final String ACTIVE = "archivedAt is null";

  @Override
  public List<Warehouse> getAll() {
    return find(ACTIVE, Sort.by("id")).list().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    if (warehouse.createdAt == null) {
      warehouse.createdAt = LocalDateTime.now();
    }

    DbWarehouse dbWarehouse = DbWarehouse.from(warehouse);
    persist(dbWarehouse);

    // give the caller back the generated identifier
    warehouse.id = dbWarehouse.id;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = resolveEntity(warehouse);
    dbWarehouse.applyFrom(warehouse);
    // no explicit persist needed: the entity is managed and flushed on commit
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    delete(resolveEntity(warehouse));
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) {
      return null;
    }

    return find("businessUnitCode = ?1 and " + ACTIVE, buCode)
            .firstResultOptional()
            .map(DbWarehouse::toWarehouse)
            .orElse(null);
  }

  @Override
  public Warehouse findActiveById(Long id) {
    if (id == null) {
      return null;
    }

    return find("id = ?1 and " + ACTIVE, id)
            .firstResultOptional()
            .map(DbWarehouse::toWarehouse)
            .orElse(null);
  }

  /**
   * Finds the row backing a domain warehouse: by surrogate id when we have one, otherwise by the
   * business unit code of the active warehouse.
   */
  private DbWarehouse resolveEntity(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
            warehouse.id != null
                    ? findById(warehouse.id)
                    : find("businessUnitCode = ?1 and " + ACTIVE, warehouse.businessUnitCode).firstResult();

    if (dbWarehouse == null) {
      throw new ResourceNotFoundException(
              "Warehouse " + warehouse.businessUnitCode + " does not exist.");
    }

    return dbWarehouse;
  }
}
