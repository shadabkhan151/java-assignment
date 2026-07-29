package com.fulfilment.application.monolith.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps a domain rule violation to <b>400 Bad Request</b>.
 *
 * <p>409 Conflict would arguably describe "business unit code already taken" better, but the
 * OpenAPI contract of the Warehouse API only documents 400/404, so we stick to the published
 * contract.
 */
@Provider
public class BusinessRuleViolationExceptionMapper
    implements ExceptionMapper<BusinessRuleViolationException> {

  @Override
  public Response toResponse(BusinessRuleViolationException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(ErrorPayload.of(400, exception))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
