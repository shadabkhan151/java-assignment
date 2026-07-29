package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * REST adapter for the Warehouse API generated from {@code warehouse-openapi.yaml}.
 *
 * <p>Responsibilities kept here on purpose: mapping the transport bean to/from the domain model,
 * and defining the transaction boundary. Every business rule lives in the use cases, which are
 * injected through their ports, so they can be unit tested without a container or a database.
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseStore warehouseStore;

  @Inject CreateWarehouseOperation createWarehouseOperation;

  @Inject ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  @ResponseStatus(201)
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomain(data);

    createWarehouseOperation.create(warehouse);

    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    return toWarehouseResponse(resolveActiveOrFail(id));
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    archiveWarehouseOperation.archive(resolveActiveOrFail(id));
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
          String businessUnitCode, @NotNull Warehouse data) {

    var newWarehouse = toDomain(data);

    // The business unit code is owned by the path: the body may repeat it, but not contradict it.
    if (newWarehouse.businessUnitCode != null
            && !newWarehouse.businessUnitCode.isBlank()
            && !newWarehouse.businessUnitCode.equals(businessUnitCode)) {
      throw new BusinessRuleViolationException(
              "The business unit code in the payload ("
                      + newWarehouse.businessUnitCode
                      + ") does not match the one being replaced ("
                      + businessUnitCode
                      + ").");
    }
    newWarehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(newWarehouse);

    return toWarehouseResponse(newWarehouse);
  }

  /**
   * The OpenAPI contract types the path parameter as a plain string named {@code id}, while the
   * Warehouse schema carries both an {@code id} and a {@code businessUnitCode}. To avoid guessing
   * wrong, we accept both: the business unit code (the identifier the business actually uses) and,
   * as a fallback, the numeric surrogate id.
   */
  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse resolveActiveOrFail(
          String id) {

    if (id == null || id.isBlank()) {
      throw new BusinessRuleViolationException("A warehouse identifier is required.");
    }

    var warehouse = warehouseStore.findByBusinessUnitCode(id);

    if (warehouse == null) {
      Long numericId = toLongOrNull(id);
      if (numericId != null) {
        warehouse = warehouseStore.findActiveById(numericId);
      }
    }

    if (warehouse == null) {
      throw new ResourceNotFoundException("Warehouse unit " + id + " does not exist or is archived.");
    }

    return warehouse;
  }

  private static Long toLongOrNull(String value) {
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomain(
          Warehouse data) {

    if (data == null) {
      throw new BusinessRuleViolationException("A warehouse payload is required.");
    }

    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();

    return warehouse;
  }

  private Warehouse toWarehouseResponse(
          com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    if (warehouse.id != null) {
      response.setId(String.valueOf(warehouse.id));
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
