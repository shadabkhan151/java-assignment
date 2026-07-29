package com.fulfilment.application.monolith.exceptions;

/** Raised when an entity referenced by the request does not exist (or is no longer active). */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
