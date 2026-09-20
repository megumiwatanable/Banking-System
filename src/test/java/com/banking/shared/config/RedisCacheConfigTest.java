package com.banking.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.user.api.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class RedisCacheConfigTest {

  @Test
  void preservesRecordTypeWhenReadingCachedResponse() {
    ObjectMapper mapper = RedisCacheConfig.createCacheObjectMapper(new ObjectMapper());
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
    UserResponse response = new UserResponse(1L, "Megumi Watanabe", "megumi@example.com");

    Object cachedValue = serializer.deserialize(serializer.serialize(response));

    assertThat(cachedValue).isEqualTo(response).isInstanceOf(UserResponse.class);
  }
}
