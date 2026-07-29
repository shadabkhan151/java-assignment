package com.fulfilment.application.monolith.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps a missing/archived resource to <b>404 Not Found</b>. */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

  @Override
  public Response toResponse(ResourceNotFoundException exception) {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(ErrorPayload.of(404, exception))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
