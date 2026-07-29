package com.fulfilment.application.monolith.exceptions;

/**
 * Error body shared by the exception mappers. It intentionally mirrors the shape already produced
 * by the pre-existing {@code ErrorMapper} classes so that clients see one consistent contract.
 */
public class ErrorPayload {

  public String exceptionType;
  public int code;
  public String error;

  public ErrorPayload() {}

  public static ErrorPayload of(int code, Exception exception) {
    ErrorPayload payload = new ErrorPayload();
    payload.exceptionType = exception.getClass().getName();
    payload.code = code;
    payload.error = exception.getMessage();
    return payload;
  }
}
