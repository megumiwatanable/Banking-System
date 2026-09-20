package com.banking.credit.domain;

import com.banking.user.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_ratings")
@Getter
@Setter
@NoArgsConstructor
public class CreditRating {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  private int score;

  @Column(nullable = false, length = 5)
  private String grade;

  @Column(nullable = false, length = 1000)
  private String explanation;

  @Column(name = "evaluated_at", nullable = false)
  private LocalDateTime evaluatedAt;

  @PrePersist
  @PreUpdate
  void evaluate() {
    evaluatedAt = LocalDateTime.now();
  }
}
