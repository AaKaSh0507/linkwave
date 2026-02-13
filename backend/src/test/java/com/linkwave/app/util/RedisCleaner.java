package com.linkwave.app.util;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCleaner {

  private final StringRedisTemplate stringRedisTemplate;

  public RedisCleaner(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void flushAll() {
    if (stringRedisTemplate == null || stringRedisTemplate.getConnectionFactory() == null) {
      return;
    }

    try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection()) {
      connection.serverCommands().flushDb();
    }
  }
}
