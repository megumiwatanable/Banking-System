package com.banking.transaction.application;

import com.banking.transaction.api.dto.TransactionResponse;
import java.util.List;

public interface TransactionService {
  public List<TransactionResponse> getAllTransactions();

  TransactionResponse getTransactionById(Long transactionId);

  List<TransactionResponse> getTransactionForAcc(Long accountId);
}
