package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Creates a warehouse, enforcing the constraints described in the assignment:
 *
 * <ol>
 *   <li>the business unit code must not be in use by an active warehouse;
 *   <li>the location must exist;
 *   <li>the location must still have a free warehouse slot;
 *   <li>the location's maximum capacity must not be exceeded by the sum of its warehouses;
 *   <li>the warehouse must be able to hold the stock it is created with.
 * </ol>
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    validatePayload(warehouse);

    // 1. business unit code verification
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new BusinessRuleViolationException(
              "A warehouse with business unit code "
                      + warehouse.businessUnitCode
                      + " already exists. Use the replacement endpoint to take over a business unit.");
    }

    // 2. location validation
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new BusinessRuleViolationException(
              "Location " + warehouse.location + " is not a valid location.");
    }
    // store the canonical spelling of the location
    warehouse.location = location.identification;

    List<Warehouse> warehousesAtLocation = activeWarehousesAt(location.identification);

    // 3. creation feasibility: is there a free slot at this location?
    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new BusinessRuleViolationException(
              "Location "
                      + location.identification
                      + " already reached its maximum number of warehouses ("
                      + location.maxNumberOfWarehouses
                      + ").");
    }

    // 4. capacity validation against the location
    int capacityInUse =
            warehousesAtLocation.stream().mapToInt(w -> w.capacity == null ? 0 : w.capacity).sum();
    if (capacityInUse + warehouse.capacity > location.maxCapacity) {
      throw new BusinessRuleViolationException(
              "Capacity "
                      + warehouse.capacity
                      + " exceeds the remaining capacity of location "
                      + location.identification
                      + " ("
                      + (location.maxCapacity - capacityInUse)
                      + " left out of "
                      + location.maxCapacity
                      + ").");
    }

    // 5. the warehouse must be able to hold its own stock
    if (warehouse.stock > warehouse.capacity) {
      throw new BusinessRuleViolationException(
              "Stock " + warehouse.stock + " exceeds the warehouse capacity of " + warehouse.capacity + ".");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }

  private List<Warehouse> activeWarehousesAt(String locationIdentification) {
    return warehouseStore.getAll().stream()
            .filter(w -> !w.isArchived())
            .filter(w -> locationIdentification.equalsIgnoreCase(w.location))
            .toList();
  }

  private void validatePayload(Warehouse warehouse) {
    if (warehouse == null) {
      throw new BusinessRuleViolationException("A warehouse payload is required.");
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new BusinessRuleViolationException("Business unit code is required.");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new BusinessRuleViolationException("Location is required.");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new BusinessRuleViolationException("Capacity is required and must be greater than 0.");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new BusinessRuleViolationException("Stock is required and cannot be negative.");
    }
  }
}
