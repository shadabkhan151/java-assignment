package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore =
        new InMemoryWarehouseStore(
            new Warehouse("MWH.001", "ZWOLLE-002", 30, 10),
            new Warehouse("MWH.023", "TILBURG-001", 30, 27));

    LocationGateway locationGateway = new LocationGateway();
    replaceWarehouseUseCase =
        new ReplaceWarehouseUseCase(
            warehouseStore,
            new ArchiveWarehouseUseCase(warehouseStore),
            new CreateWarehouseUseCase(warehouseStore, locationGateway));
  }

  @Test
  void shouldArchiveThePreviousWarehouseAndCreateTheNewOneWithTheSameBusinessUnitCode() {
    Warehouse replacement = new Warehouse("MWH.001", "ZWOLLE-002", 50, 10);

    replaceWarehouseUseCase.replace(replacement);

    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.001");
    assertNotNull(active);
    assertEquals(50, active.capacity.intValue());
    assertEquals(10, active.stock.intValue());
    assertNotNull(active.createdAt);

    // history is preserved: two rows for the same business unit, exactly one of them active
    assertEquals(
        2,
        warehouseStore.everything().stream()
            .filter(w -> "MWH.001".equals(w.businessUnitCode))
            .count());
    assertEquals(
        1,
        warehouseStore.everything().stream()
            .filter(w -> "MWH.001".equals(w.businessUnitCode) && w.isArchived())
            .count());
  }

  @Test
  void shouldFailWhenThereIsNoActiveWarehouseForThatBusinessUnitCode() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> replaceWarehouseUseCase.replace(new Warehouse("MWH.999", "ZWOLLE-002", 30, 10)));
  }

  @Test
  void shouldRejectANewCapacityThatCannotAccommodateThePreviousStock() {
    var exception =
        assertThrows(
            BusinessRuleViolationException.class,
            () -> replaceWarehouseUseCase.replace(new Warehouse("MWH.001", "ZWOLLE-002", 5, 10)));

    assertTrue(exception.getMessage().contains("cannot accommodate"));
    // nothing changed
    assertNotNull(warehouseStore.findByBusinessUnitCode("MWH.001"));
  }

  @Test
  void shouldRejectAStockThatDoesNotMatchTheReplacedWarehouse() {
    var exception =
        assertThrows(
            BusinessRuleViolationException.class,
            () -> replaceWarehouseUseCase.replace(new Warehouse("MWH.001", "ZWOLLE-002", 50, 20)));

    assertTrue(exception.getMessage().contains("must match"));
    assertNotNull(warehouseStore.findByBusinessUnitCode("MWH.001"));
  }

  @Test
  void shouldStillEnforceTheCreationRulesOfTheTargetLocation() {
    // TILBURG-001 accepts a single warehouse and MWH.023 already occupies it.
    // NOTE: rolling the archiving back is the job of the transaction opened by the REST adapter,
    // which is covered by WarehouseResourceTest; here we only assert the rule is enforced.
    assertThrows(
        BusinessRuleViolationException.class,
        () -> replaceWarehouseUseCase.replace(new Warehouse("MWH.001", "TILBURG-001", 50, 10)));
  }
}
