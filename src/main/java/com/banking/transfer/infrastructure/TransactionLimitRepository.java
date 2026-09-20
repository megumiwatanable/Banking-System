package com.banking.transfer.infrastructure;
import com.banking.transfer.domain.TransactionLimit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {
  Optional<TransactionLimit> findByUserIdAndCurrency(Long userId, String currency);
}
