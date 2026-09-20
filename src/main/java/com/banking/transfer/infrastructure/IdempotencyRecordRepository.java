package com.banking.transfer.infrastructure;
import com.banking.transfer.domain.IdempotencyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
  Optional<IdempotencyRecord> findByUserIdAndKey(Long userId, String key);
}
