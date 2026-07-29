package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Replaces the active warehouse of a business unit by a new one that takes over the same business
 * unit code.
 *
 * <p>The flow is deliberately expressed as <b>archive the current one, then create the new one</b>
 * by reusing the existing use cases:
 *
 * <ul>
 *   <li>no validation rule is duplicated — the new warehouse goes through exactly the same location,
 *       feasibility and capacity checks as any other creation;
 *   <li>because the old warehouse is archived first, the "business unit code must be free" check
 *       passes naturally, and the location capacity is computed with the old warehouse already out
 *       of the way.
 * </ul>
 *
 * <p>The caller (the REST adapter) runs this inside a single transaction, so a failure while
 * creating the new warehouse rolls the archiving back: we never end up with an archived business
 * unit and no successor.
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final CreateWarehouseOperation createWarehouseOperation;

  @Inject
  public ReplaceWarehouseUseCase(
          WarehouseStore warehouseStore,
          ArchiveWarehouseOperation archiveWarehouseOperation,
          CreateWarehouseOperation createWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.createWarehouseOperation = createWarehouseOperation;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null
            || newWarehouse.businessUnitCode == null
            || newWarehouse.businessUnitCode.isBlank()) {
      throw new BusinessRuleViolationException(
              "Business unit code is required to replace a warehouse.");
    }

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);

    if (current == null) {
      throw new ResourceNotFoundException(
              "There is no active warehouse with business unit code "
                      + newWarehouse.businessUnitCode
                      + " to be replaced.");
    }

    if (newWarehouse.capacity == null || newWarehouse.stock == null) {
      throw new BusinessRuleViolationException(
              "Capacity and stock are required to replace a warehouse.");
    }

    int previousStock = current.stock == null ? 0 : current.stock;

    // Capacity accommodation: the successor must be able to take over what is stored today.
    if (newWarehouse.capacity < previousStock) {
      throw new BusinessRuleViolationException(
              "The new warehouse capacity ("
                      + newWarehouse.capacity
                      + ") cannot accommodate the stock of the warehouse being replaced ("
                      + previousStock
                      + ").");
    }

    // Stock matching: nothing may be lost or invented during the hand over.
    if (newWarehouse.stock != previousStock) {
      throw new BusinessRuleViolationException(
              "The stock of the new warehouse ("
                      + newWarehouse.stock
                      + ") must match the stock of the warehouse being replaced ("
                      + previousStock
                      + ").");
    }

    archiveWarehouseOperation.archive(current);

    // Reuses every creation rule (location exists, free slot, location capacity, stock <= capacity).
    newWarehouse.id = null;
    newWarehouse.archivedAt = null;
    createWarehouseOperation.create(newWarehouse);
  }
}
