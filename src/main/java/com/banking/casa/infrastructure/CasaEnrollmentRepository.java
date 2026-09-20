package com.banking.casa.infrastructure;

import com.banking.casa.domain.CasaEnrollment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CasaEnrollmentRepository extends JpaRepository<CasaEnrollment, Long> {
  Optional<CasaEnrollment> findByUserEmail(String email);
}
