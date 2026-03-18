package com.xray.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public boolean allow(String key) {
    return getBucket(key).tryConsume(1);
  }

  private Bucket getBucket(String key) {
    return buckets.computeIfAbsent(key, k -> {
      Refill refill = Refill.greedy(20, Duration.ofMinutes(1));
      Bandwidth limit = Bandwidth.classic(20, refill);
      return Bucket.builder().addLimit(limit).build();
    });
  }
}
