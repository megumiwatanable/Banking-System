package com.banking.customer.infrastructure;

import com.banking.customer.domain.CustomerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
  Optional<CustomerProfile> findByUserEmail(String email);

  boolean existsByCustomerNumber(String customerNumber);
}
