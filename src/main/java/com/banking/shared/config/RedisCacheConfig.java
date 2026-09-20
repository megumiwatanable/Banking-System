package com.banking.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig {

  @Bean
  public RedisCacheManager cacheManager(
      RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {

    ObjectMapper mapper = createCacheObjectMapper(objectMapper);

    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

    RedisCacheConfiguration configuration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer));

    return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(configuration).build();
  }

  static ObjectMapper createCacheObjectMapper(ObjectMapper objectMapper) {
    ObjectMapper mapper = objectMapper.copy();

    // Cached API responses are records, which are final classes. Type metadata must also be
    // written for final values so Redis can deserialize them to the response type instead of a
    // LinkedHashMap.
    mapper.activateDefaultTyping(
        mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.EVERYTHING);
    return mapper;
  }
}
