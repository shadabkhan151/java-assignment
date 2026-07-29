package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArchiveWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore(new Warehouse("MWH.001", "ZWOLLE-001", 40, 10));
    archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void shouldArchiveAnActiveWarehouseKeepingItsHistory() {
    archiveWarehouseUseCase.archive(warehouseStore.findByBusinessUnitCode("MWH.001"));

    assertNull(warehouseStore.findByBusinessUnitCode("MWH.001"));
    assertEquals(0, warehouseStore.getAll().size());
    // the row is still there, stamped
    assertEquals(1, warehouseStore.everything().size());
    assertNotNull(warehouseStore.everything().get(0).archivedAt);
  }

  @Test
  void shouldFailWhenTheWarehouseDoesNotExist() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> archiveWarehouseUseCase.archive(new Warehouse("MWH.999", "ZWOLLE-001", 10, 0)));
  }

  @Test
  void shouldFailWhenTheWarehouseWasAlreadyArchived() {
    Warehouse warehouse = warehouseStore.findByBusinessUnitCode("MWH.001");
    archiveWarehouseUseCase.archive(warehouse);

    assertThrows(ResourceNotFoundException.class, () -> archiveWarehouseUseCase.archive(warehouse));
  }

  @Test
  void shouldRejectAnEmptyBusinessUnitCode() {
    assertThrows(
        BusinessRuleViolationException.class,
        () -> archiveWarehouseUseCase.archive(new Warehouse("  ", "ZWOLLE-001", 10, 0)));
  }
}
