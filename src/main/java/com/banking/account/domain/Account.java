package com.banking.account.domain;

import com.banking.transaction.domain.Transaction;
import com.banking.user.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String accountNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountType accountType;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal holdAmount = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  private String currency = "VND";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status = AccountStatus.ACTIVE;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @OneToMany(mappedBy = "senderAccount", cascade = CascadeType.ALL)
  @JsonIgnore
  private List<Transaction> sentTransactions = new ArrayList<>();

  @OneToMany(mappedBy = "receiverAccount", cascade = CascadeType.ALL)
  @JsonIgnore
  private List<Transaction> receivedTransactions = new ArrayList<>();

  @Version @JsonIgnore private Long version;

  public Account(String accountNumber, AccountType accountType, User user) {
    this.accountNumber = accountNumber;
    this.accountType = accountType;
    this.user = user;
    this.balance = BigDecimal.ZERO;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }

  @Transient
  public BigDecimal getAvailableBalance() {
    return balance.subtract(holdAmount);
  }
}
