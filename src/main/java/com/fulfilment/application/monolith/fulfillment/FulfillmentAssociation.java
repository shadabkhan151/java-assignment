package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * States that a {@link DbWarehouse} acts as a fulfilment unit of a given {@link Product} for a
 * given {@link Store}.
 *
 * <p>The triple is the natural key, enforced in the database as well: the application rules below
 * are checked in the use case, but a unique constraint is the only thing that survives two
 * concurrent requests.
 */
@Entity
@Table(
    name = "warehouse_product_store_fulfillment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_fulfillment_store_product_warehouse",
            columnNames = {"store_id", "product_id", "warehouse_id"}))
public class FulfillmentAssociation {

  @Id @GeneratedValue public Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  public Store store;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  public Product product;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  public DbWarehouse warehouse;

  public LocalDateTime createdAt;

  public FulfillmentAssociation() {}

  public FulfillmentAssociation(Store store, Product product, DbWarehouse warehouse) {
    this.store = store;
    this.product = product;
    this.warehouse = warehouse;
    this.createdAt = LocalDateTime.now();
  }
}
