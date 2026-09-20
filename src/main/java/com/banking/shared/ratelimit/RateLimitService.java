package com.banking.shared.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class RateLimitService {
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(100000).build();

  public Bucket resolveBucket(
      String key, int capacity, int refillTokens, Long refillPeriodSeconds) {
    return buckets.get(key, k -> createBucket(capacity, refillTokens, refillPeriodSeconds));
  }

  public Bucket createBucket(int capacity, int refillTokens, Long refillPeriodSeconds) {
    Bandwidth limit =
        Bandwidth.classic(
            capacity, Refill.greedy(refillTokens, Duration.ofSeconds(refillPeriodSeconds)));
    return Bucket.builder().addLimit(limit).build();
  }
}
