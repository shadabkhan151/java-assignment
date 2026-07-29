package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.exceptions.BusinessRuleViolationException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Associates warehouses as fulfilment units of products for stores, enforcing:
 *
 * <ol>
 *   <li>a product can be fulfilled by at most <b>2</b> warehouses per store;
 *   <li>a store can be fulfilled by at most <b>3</b> warehouses;
 *   <li>a warehouse can store at most <b>5</b> product types.
 * </ol>
 */
@ApplicationScoped
public class FulfillmentUseCase {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  @Inject FulfillmentAssociationRepository associations;

  @Inject ProductRepository productRepository;

  @Inject WarehouseRepository warehouseRepository;

  @Transactional
  public FulfillmentAssociation associate(FulfillmentRequest request) {
    if (request == null
        || request.storeId == null
        || request.productId == null
        || request.warehouseBusinessUnitCode == null
        || request.warehouseBusinessUnitCode.isBlank()) {
      throw new BusinessRuleViolationException(
          "storeId, productId and warehouseBusinessUnitCode are required.");
    }

    Store store = Store.findById(request.storeId);
    if (store == null) {
      throw new ResourceNotFoundException("Store with id " + request.storeId + " does not exist.");
    }

    Product product = productRepository.findById(request.productId);
    if (product == null) {
      throw new ResourceNotFoundException(
          "Product with id " + request.productId + " does not exist.");
    }

    DbWarehouse warehouse =
        warehouseRepository
            .find(
                "businessUnitCode = ?1 and archivedAt is null", request.warehouseBusinessUnitCode)
            .firstResult();
    if (warehouse == null) {
      throw new ResourceNotFoundException(
          "There is no active warehouse with business unit code "
              + request.warehouseBusinessUnitCode
              + ".");
    }

    if (associations.exists(store.id, product.id, warehouse.id)) {
      throw new BusinessRuleViolationException(
          "Warehouse "
              + warehouse.businessUnitCode
              + " already fulfils product "
              + product.name
              + " for store "
              + store.name
              + ".");
    }

    // 1. at most 2 warehouses fulfilling the same product for the same store
    if (associations.countWarehousesFulfillingProductAtStore(store.id, product.id)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new BusinessRuleViolationException(
          "Product "
              + product.name
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses for store "
              + store.name
              + ".");
    }

    // 2. at most 3 warehouses fulfilling a store (a warehouse that already serves the store is
    //    not a new one, so it does not consume a slot)
    if (!associations.warehouseAlreadyFulfillsStore(store.id, warehouse.id)
        && associations.countWarehousesFulfillingStore(store.id) >= MAX_WAREHOUSES_PER_STORE) {
      throw new BusinessRuleViolationException(
          "Store "
              + store.name
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses.");
    }

    // 3. at most 5 product types per warehouse (same reasoning for an already stored product)
    if (!associations.warehouseAlreadyStoresProduct(warehouse.id, product.id)
        && associations.countProductTypesInWarehouse(warehouse.id)
            >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new BusinessRuleViolationException(
          "Warehouse "
              + warehouse.businessUnitCode
              + " already stores the maximum of "
              + MAX_PRODUCT_TYPES_PER_WAREHOUSE
              + " product types.");
    }

    FulfillmentAssociation association = new FulfillmentAssociation(store, product, warehouse);
    associations.persist(association);

    return association;
  }

  @Transactional
  public void dissociate(Long id) {
    FulfillmentAssociation association = associations.findById(id);
    if (association == null) {
      throw new ResourceNotFoundException("Fulfilment association " + id + " does not exist.");
    }
    associations.delete(association);
  }
}
