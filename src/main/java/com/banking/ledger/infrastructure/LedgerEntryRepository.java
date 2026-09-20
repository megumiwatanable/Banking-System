package com.banking.ledger.infrastructure;

import com.banking.ledger.domain.LedgerEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
  List<LedgerEntry> findByTransactionIdOrderById(Long transactionId);
}
