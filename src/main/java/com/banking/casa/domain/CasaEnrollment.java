package com.banking.casa.domain;

import com.banking.account.domain.Account;
import com.banking.user.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "casa_enrollments")
@Getter
@Setter
@NoArgsConstructor
public class CasaEnrollment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "settlement_account_id", nullable = false)
  private Account settlementAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "package_type", nullable = false)
  private CasaPackage packageType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CasaStatus status;

  @Column(name = "enrolled_at", nullable = false)
  private LocalDateTime enrolledAt;

  @PrePersist
  void create() {
    enrolledAt = LocalDateTime.now();
  }
}
