package com.banking.shared.error;

public class InvalidAccountOperationException extends RuntimeException {
  public InvalidAccountOperationException(String message) {
    super(message);
  }
}
