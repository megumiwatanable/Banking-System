package com.banking.transaction.domain;

public enum TransactionStatus {
  PENDING,
  INITIATED,
  VALIDATING,
  PROCESSING,
  SUCCESS,
  FAILED,
  REVERSING,
  REVERSED
}
