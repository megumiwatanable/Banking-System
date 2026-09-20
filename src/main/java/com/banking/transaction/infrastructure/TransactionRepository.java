package com.banking.transaction.infrastructure;

import com.banking.transaction.domain.Transaction;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findBySenderAccount_IdOrReceiverAccount_IdOrderByTransactionTimeDesc(
      Long senderAccountId, Long receiverAccountId);

  List<Transaction> findBySenderAccountUserEmailOrReceiverAccountUserEmail(
      String senderEmail, String receiverEmail);

  @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.senderAccount.id = :accountId and t.status = com.banking.transaction.domain.TransactionStatus.SUCCESS and t.transactionTime >= :since")
  BigDecimal sumSuccessfulOutgoingSince(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);
}
