package com.banking.transaction.domain;

public enum TransactionType {
  DEPOSIT,
  WITHDRAW,
  TRANSFER,
  INTERNAL_TRANSFER,
  INTERBANK_TRANSFER,
  BILL_PAYMENT,
  CARD_PAYMENT,
  LOAN_DISBURSEMENT,
  LOAN_REPAYMENT,
  FEE,
  REVERSAL
}
