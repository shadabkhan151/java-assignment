package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.LocalDateTime;

public class Warehouse {

  /**
   * Surrogate identifier assigned by the persistence layer. The <em>business</em> identity of a
   * warehouse is the {@link #businessUnitCode}; this field only exists because the published API
   * exposes an {@code id} and the archive/get endpoints accept it.
   */
  public Long id;

  // unique identifier (unique among the *active* warehouses: an archived warehouse keeps its code
  // so that the business unit history is preserved)
  public String businessUnitCode;

  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  public Warehouse() {}

  public Warehouse(String businessUnitCode, String location, Integer capacity, Integer stock) {
    this.businessUnitCode = businessUnitCode;
    this.location = location;
    this.capacity = capacity;
    this.stock = stock;
  }

  public boolean isArchived() {
    return archivedAt != null;
  }
}
