package com.banking.asset.domain;

import com.banking.user.domain.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_assets")
@Getter
@Setter
@NoArgsConstructor
public class CustomerAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "asset_type", nullable = false)
  private AssetType assetType;

  @Column(name = "asset_name", nullable = false)
  private String assetName;

  @Column(name = "estimated_value", nullable = false, precision = 19, scale = 2)
  private BigDecimal estimatedValue;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(length = 1000)
  private String description;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void create() {
    createdAt = updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void update() {
    updatedAt = LocalDateTime.now();
  }
}
