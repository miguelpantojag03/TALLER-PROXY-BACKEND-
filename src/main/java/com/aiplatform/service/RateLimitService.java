package com.aiplatform.service;

import com.aiplatform.entity.Plan;
import com.aiplatform.entity.User;
import com.aiplatform.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Proxy de Rate Limiting usando Redis con contadores atómicos.
 *
 * Estrategia:
 *  - Clave por minuto:  ratelimit:{userId}:minute:{yyyy-MM-dd-HH-mm}
 *  - Clave por día:     ratelimit:{userId}:day:{yyyy-MM-dd}
 *
 * Cada clave tiene TTL automático → no hay limpieza manual.
 * Las operaciones INCR de Redis son atómicas → thread-safe en entornos distribuidos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Verifica y consume un token de rate limit para el usuario.
     * Lanza RateLimitExceededException si se supera algún límite.
     */
    public void checkAndConsume(User user) {
        Plan plan = user.getPlan();
        long userId = user.getId();

        // --- Verificar límite por minuto ---
        String minuteKey = buildMinuteKey(userId);
        long minuteCount = increment(minuteKey, Duration.ofMinutes(2)); // TTL 2 min por seguridad

        if (minuteCount > plan.getMaxRequestsPerMinute()) {
            log.warn("Rate limit MINUTE exceeded for user {} (plan={}): {}/{}",
                    userId, plan.getName(), minuteCount, plan.getMaxRequestsPerMinute());
            throw new RateLimitExceededException("MINUTE",
                    String.format("Rate limit exceeded: %d requests/minute allowed for plan %s. Retry in %d seconds.",
                            plan.getMaxRequestsPerMinute(), plan.getName(), secondsUntilNextMinute()));
        }

        // --- Verificar límite diario ---
        String dayKey = buildDayKey(userId);
        long dayCount = increment(dayKey, Duration.ofDays(2)); // TTL 2 días por seguridad

        if (dayCount > plan.getMaxRequestsPerDay()) {
            // Revertir el incremento de minuto para no penalizar
            decrement(minuteKey);
            log.warn("Rate limit DAY exceeded for user {} (plan={}): {}/{}",
                    userId, plan.getName(), dayCount, plan.getMaxRequestsPerDay());
            throw new RateLimitExceededException("DAY",
                    String.format("Daily quota exceeded: %d requests/day allowed for plan %s. Resets at midnight.",
                            plan.getMaxRequestsPerDay(), plan.getName()));
        }

        log.debug("Rate limit OK for user {}: minute={}/{}, day={}/{}",
                userId, minuteCount, plan.getMaxRequestsPerMinute(),
                dayCount, plan.getMaxRequestsPerDay());
    }

    /**
     * Obtiene el conteo actual sin modificarlo (para el dashboard).
     */
    public long getCurrentMinuteCount(long userId) {
        String key = buildMinuteKey(userId);
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0L;
    }

    public long getCurrentDayCount(long userId) {
        String key = buildDayKey(userId);
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0L;
    }

    // --- Helpers ---

    private long increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // Primera vez → establecer TTL
            redisTemplate.expire(key, ttl.toSeconds(), TimeUnit.SECONDS);
        }
        return count != null ? count : 1L;
    }

    private void decrement(String key) {
        redisTemplate.opsForValue().decrement(key);
    }

    private String buildMinuteKey(long userId) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("ratelimit:%d:minute:%d-%02d-%02d-%02d-%02d",
                userId, now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute());
    }

    private String buildDayKey(long userId) {
        LocalDate today = LocalDate.now();
        return String.format("ratelimit:%d:day:%s", userId, today);
    }

    private long secondsUntilNextMinute() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        return ChronoUnit.SECONDS.between(now, nextMinute);
    }
}
