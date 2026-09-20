package com.banking.shared.error;

public class AccountLockTimeoutException extends RuntimeException {
  public AccountLockTimeoutException(String message) {
    super(message);
  }
}
