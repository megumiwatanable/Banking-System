package com.banking.transaction.infrastructure;

import com.banking.transaction.domain.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findBySenderAccount_IdOrReceiverAccount_IdOrderByTransactionTimeDesc(
      Long senderAccountId, Long receiverAccountId);

  List<Transaction> findBySenderAccountUserEmailOrReceiverAccountUserEmail(
      String senderEmail, String receiverEmail);
}
