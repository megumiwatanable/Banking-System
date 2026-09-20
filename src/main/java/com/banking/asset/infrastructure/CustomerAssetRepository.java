package com.banking.asset.infrastructure;

import com.banking.asset.domain.CustomerAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAssetRepository extends JpaRepository<CustomerAsset, Long> {
  List<CustomerAsset> findByUserEmailOrderByUpdatedAtDesc(String email);

  Optional<CustomerAsset> findByIdAndUserEmail(Long id, String email);
}
