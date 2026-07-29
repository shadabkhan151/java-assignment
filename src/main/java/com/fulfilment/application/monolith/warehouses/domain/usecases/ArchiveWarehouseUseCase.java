package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;

/**
 * Archives a warehouse: a soft delete that stamps {@code archivedAt} and keeps the row, so the
 * history of the business unit code (and of everything that was costed against it) is preserved.
 */
@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Inject
  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    if (warehouse == null
            || warehouse.businessUnitCode == null
            || warehouse.businessUnitCode.isBlank()) {
      throw new BusinessRuleViolationException("Business unit code is required to archive a warehouse.");
    }

    Warehouse current = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);

    if (current == null) {
      throw new ResourceNotFoundException(
              "There is no active warehouse with business unit code " + warehouse.businessUnitCode + ".");
    }

    current.archivedAt = LocalDateTime.now();
    warehouseStore.update(current);

    // reflect the change on the instance the caller handed us
    warehouse.id = current.id;
    warehouse.archivedAt = current.archivedAt;
  }
}
