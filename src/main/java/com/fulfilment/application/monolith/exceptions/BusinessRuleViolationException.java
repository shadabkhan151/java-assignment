package com.fulfilment.application.monolith.exceptions;

/**
 * Raised when a request is syntactically fine but violates a domain rule (duplicated business unit
 * code, unknown location, capacity exceeded, ...).
 *
 * <p>It lives outside the {@code warehouses.domain} package on purpose: it is reused by every
 * module (warehouses, fulfillment) and it carries no framework/JAX-RS dependency, so the domain
 * layer stays transport agnostic. The translation to an HTTP status code happens in a single {@link
 * BusinessRuleViolationExceptionMapper}.
 */
public class BusinessRuleViolationException extends RuntimeException {

  public BusinessRuleViolationException(String message) {
    super(message);
  }
}
