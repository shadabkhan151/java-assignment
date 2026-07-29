package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Counting queries backing the fulfilment constraints. They are expressed as aggregates in the
 * database rather than by loading collections in memory: the checks stay O(1) as the association
 * table grows.
 */
@ApplicationScoped
public class FulfillmentAssociationRepository implements PanacheRepository<FulfillmentAssociation> {

  private static final String FETCH_ALL =
      "select f from FulfillmentAssociation f "
          + "join fetch f.store s join fetch f.product p join fetch f.warehouse w ";

  public List<FulfillmentAssociation> listAllOrdered() {
    return getEntityManager()
        .createQuery(FETCH_ALL + "order by s.id, p.id, w.id", FulfillmentAssociation.class)
        .getResultList();
  }

  public List<FulfillmentAssociation> listByStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            FETCH_ALL + "where s.id = :storeId order by p.id, w.id", FulfillmentAssociation.class)
        .setParameter("storeId", storeId)
        .getResultList();
  }

  public boolean exists(Long storeId, Long productId, Long warehouseId) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouse.id = ?3",
            storeId,
            productId,
            warehouseId)
        > 0;
  }

  /** How many distinct warehouses already fulfil this product for this store (limit: 2). */
  public long countWarehousesFulfillingProductAtStore(Long storeId, Long productId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.warehouse.id) from FulfillmentAssociation f "
                + "where f.store.id = :storeId and f.product.id = :productId",
            Long.class)
        .setParameter("storeId", storeId)
        .setParameter("productId", productId)
        .getSingleResult();
  }

  /** How many distinct warehouses already fulfil this store (limit: 3). */
  public long countWarehousesFulfillingStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.warehouse.id) from FulfillmentAssociation f "
                + "where f.store.id = :storeId",
            Long.class)
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  /** How many distinct product types this warehouse already stores (limit: 5). */
  public long countProductTypesInWarehouse(Long warehouseId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.product.id) from FulfillmentAssociation f "
                + "where f.warehouse.id = :warehouseId",
            Long.class)
        .setParameter("warehouseId", warehouseId)
        .getSingleResult();
  }

  public boolean warehouseAlreadyFulfillsStore(Long storeId, Long warehouseId) {
    return count("store.id = ?1 and warehouse.id = ?2", storeId, warehouseId) > 0;
  }

  public boolean warehouseAlreadyStoresProduct(Long warehouseId, Long productId) {
    return count("warehouse.id = ?1 and product.id = ?2", warehouseId, productId) > 0;
  }
}
