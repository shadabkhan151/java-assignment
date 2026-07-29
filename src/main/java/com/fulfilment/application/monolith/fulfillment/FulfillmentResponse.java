package com.fulfilment.application.monolith.fulfillment;

import java.time.LocalDateTime;

/**
 * Flat view of an association. A DTO rather than the entity itself: serializing the entity would
 * drag lazy proxies and the whole Store/Product/Warehouse graph into the response.
 */
public class FulfillmentResponse {

  public Long id;
  public Long storeId;
  public String storeName;
  public Long productId;
  public String productName;
  public String warehouseBusinessUnitCode;
  public LocalDateTime createdAt;

  public FulfillmentResponse() {}

  public static FulfillmentResponse from(FulfillmentAssociation association) {
    FulfillmentResponse response = new FulfillmentResponse();
    response.id = association.id;
    response.storeId = association.store.id;
    response.storeName = association.store.name;
    response.productId = association.product.id;
    response.productName = association.product.name;
    response.warehouseBusinessUnitCode = association.warehouse.businessUnitCode;
    response.createdAt = association.createdAt;
    return response;
  }
}
