package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests: no container, no database. The location catalogue is the real
 * {@link LocationGateway} because it is itself an in-memory, deterministic table.
 */
public class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseStore, new LocationGateway());
  }

  @Test
  void shouldCreateAWarehouseOnAValidLocation() {
    // given ZWOLLE-001 allows 1 warehouse and 40 of capacity
    Warehouse warehouse = new Warehouse("MWH.100", "ZWOLLE-001", 30, 10);

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    assertNotNull(warehouse.id);
    assertNotNull(warehouse.createdAt);
    assertEquals(1, warehouseStore.getAll().size());
    assertEquals("MWH.100", warehouseStore.findByBusinessUnitCode("MWH.100").businessUnitCode);
  }

  @Test
  void shouldStoreTheCanonicalLocationIdentification() {
    Warehouse warehouse = new Warehouse("MWH.100", "zwolle-001", 30, 10);

    createWarehouseUseCase.create(warehouse);

    assertEquals("ZWOLLE-001", warehouseStore.findByBusinessUnitCode("MWH.100").location);
  }

  @Test
  void shouldRejectAnAlreadyUsedBusinessUnitCode() {
    createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 30, 10));

    var exception =
        assertThrows(
            BusinessRuleViolationException.class,
            () -> createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-002", 30, 10)));

    assertTrue(exception.getMessage().contains("already exists"));
  }

  @Test
  void shouldRejectAnUnknownLocation() {
    assertThrows(
        BusinessRuleViolationException.class,
        () -> createWarehouseUseCase.create(new Warehouse("MWH.100", "MARS-001", 30, 10)));
  }

  @Test
  void shouldRejectWhenTheLocationHasNoFreeWarehouseSlot() {
    // ZWOLLE-001 accepts a single warehouse
    createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 20, 10));

    var exception =
        assertThrows(
            BusinessRuleViolationException.class,
            () -> createWarehouseUseCase.create(new Warehouse("MWH.101", "ZWOLLE-001", 10, 5)));

    assertTrue(exception.getMessage().contains("maximum number of warehouses"));
  }

  @Test
  void shouldRejectWhenTheLocationCapacityWouldBeExceeded() {
    // ZWOLLE-002 accepts 2 warehouses but only 50 of capacity in total
    createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-002", 30, 10));

    var exception =
        assertThrows(
            BusinessRuleViolationException.class,
            () -> createWarehouseUseCase.create(new Warehouse("MWH.101", "ZWOLLE-002", 30, 10)));

    assertTrue(exception.getMessage().contains("exceeds the remaining capacity"));
  }

  @Test
  void shouldRejectAStockBiggerThanTheCapacity() {
    var exception =
        assertThrows(
            BusinessRuleViolationException.class,
            () -> createWarehouseUseCase.create(new Warehouse("MWH.100", "AMSTERDAM-001", 20, 30)));

    assertTrue(exception.getMessage().contains("exceeds the warehouse capacity"));
  }

  @Test
  void shouldRejectIncompletePayloads() {
    assertThrows(
        BusinessRuleViolationException.class,
        () -> createWarehouseUseCase.create(new Warehouse(null, "ZWOLLE-001", 10, 1)));
    assertThrows(
        BusinessRuleViolationException.class,
        () -> createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-001", null, 1)));
    assertThrows(
        BusinessRuleViolationException.class,
        () -> createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 0, 0)));
    assertThrows(
        BusinessRuleViolationException.class,
        () -> createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 10, null)));
  }

  @Test
  void anArchivedWarehouseShouldReleaseItsBusinessUnitCodeAndLocationSlot() {
    Warehouse archived = new Warehouse("MWH.100", "ZWOLLE-001", 40, 10);
    createWarehouseUseCase.create(archived);
    archived.archivedAt = LocalDateTime.now();
    warehouseStore.update(archived);

    // same business unit code, same (single slot) location: allowed, the previous one is history
    createWarehouseUseCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 40, 10));

    assertEquals(1, warehouseStore.getAll().size());
    assertEquals(2, warehouseStore.everything().size());
  }
}
