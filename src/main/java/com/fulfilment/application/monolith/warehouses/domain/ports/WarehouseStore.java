package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

/**
 * Persistence port for warehouses.
 *
 * <p>Every lookup here is scoped to <b>active</b> (non archived) warehouses: archived rows are kept
 * for history only, and a business unit code is unique among active warehouses only.
 */
public interface WarehouseStore {

  /** @return all active warehouses. */
  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  /** @return the active warehouse holding this business unit code, or {@code null}. */
  Warehouse findByBusinessUnitCode(String buCode);

  /**
   * @return the active warehouse with this surrogate id, or {@code null}. Deliberately not named
   *     {@code findById} to avoid clashing with {@code PanacheRepository#findById} in the adapter.
   */
  Warehouse findActiveById(Long id);
}
