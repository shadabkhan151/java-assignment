package com.fulfilment.application.monolith.fulfillment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** Endpoints to manage which warehouses fulfil which products for which stores. */
@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  @Inject FulfillmentUseCase fulfillmentUseCase;

  @Inject FulfillmentAssociationRepository associations;

  @GET
  public List<FulfillmentResponse> list() {
    return associations.listAllOrdered().stream().map(FulfillmentResponse::from).toList();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfillmentResponse> listByStore(@PathParam("storeId") Long storeId) {
    return associations.listByStore(storeId).stream().map(FulfillmentResponse::from).toList();
  }

  @POST
  public Response associate(FulfillmentRequest request) {
    FulfillmentAssociation association = fulfillmentUseCase.associate(request);
    return Response.status(201).entity(FulfillmentResponse.from(association)).build();
  }

  @DELETE
  @Path("{id}")
  public Response dissociate(@PathParam("id") Long id) {
    fulfillmentUseCase.dissociate(id);
    return Response.status(204).build();
  }
}
