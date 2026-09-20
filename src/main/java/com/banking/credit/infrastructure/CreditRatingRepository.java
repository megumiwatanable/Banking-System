package com.banking.credit.infrastructure;

import com.banking.credit.domain.CreditRating;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditRatingRepository extends JpaRepository<CreditRating, Long> {
  Optional<CreditRating> findByUserEmail(String email);
}
