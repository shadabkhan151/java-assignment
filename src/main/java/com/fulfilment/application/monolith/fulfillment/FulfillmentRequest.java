package com.fulfilment.application.monolith.fulfillment;

/** Payload to associate a warehouse as fulfilment unit of a product for a store. */
public class FulfillmentRequest {

  public Long storeId;

  public Long productId;

  /** Business unit code of the (active) warehouse, e.g. {@code MWH.001}. */
  public String warehouseBusinessUnitCode;

  public FulfillmentRequest() {}
}
