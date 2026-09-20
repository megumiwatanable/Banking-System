package com.banking.ledger.domain;

import com.banking.account.domain.Account;
import com.banking.transaction.domain.Transaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor
public class LedgerEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transaction_id")
  private Transaction transaction;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id")
  private Account account;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LedgerEntryType entryType;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balanceBefore;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balanceAfter;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public LedgerEntry(
      Transaction transaction,
      Account account,
      LedgerEntryType entryType,
      BigDecimal amount,
      String currency,
      BigDecimal balanceBefore,
      BigDecimal balanceAfter) {
    this.transaction = transaction;
    this.account = account;
    this.entryType = entryType;
    this.amount = amount;
    this.currency = currency;
    this.balanceBefore = balanceBefore;
    this.balanceAfter = balanceAfter;
    this.createdAt = LocalDateTime.now();
  }
}
