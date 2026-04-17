package com.aiplatform.service;

import com.aiplatform.dto.UsageStatusDto;
import com.aiplatform.entity.RequestLog;
import com.aiplatform.entity.UsageSummary;
import com.aiplatform.entity.User;
import com.aiplatform.repository.RequestLogRepository;
import com.aiplatform.repository.UsageSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {

    private final UsageSummaryRepository usageSummaryRepository;
    private final RequestLogRepository requestLogRepository;
    private final RateLimitService rateLimitService;

    /**
     * Registra un request exitoso en PostgreSQL.
     * Actualiza el resumen diario (upsert).
     */
    @Transactional
    public void recordSuccess(User user, String prompt, String response, int tokensUsed) {
        // Log detallado
        requestLogRepository.save(RequestLog.builder()
                .user(user)
                .prompt(prompt)
                .response(response)
                .tokensUsed(tokensUsed)
                .status("SUCCESS")
                .build());

        // Resumen diario — upsert
        UsageSummary summary = usageSummaryRepository
                .findByUserAndUsageDate(user, LocalDate.now())
                .orElse(UsageSummary.builder().user(user).usageDate(LocalDate.now()).build());

        summary.setTotalRequests(summary.getTotalRequests() + 1);
        summary.setTotalTokens(summary.getTotalTokens() + tokensUsed);
        usageSummaryRepository.save(summary);
    }

    /**
     * Registra un request bloqueado por rate limit.
     */
    @Transactional
    public void recordRateLimited(User user, String prompt) {
        requestLogRepository.save(RequestLog.builder()
                .user(user)
                .prompt(prompt)
                .status("RATE_LIMITED")
                .build());
    }

    /**
     * Construye el DTO de estado de consumo para el frontend.
     * Combina datos de Redis (tiempo real) con PostgreSQL (histórico).
     */
    public UsageStatusDto getUsageStatus(User user) {
        var plan = user.getPlan();
        long userId = user.getId();

        // Datos en tiempo real desde Redis
        long minuteCount = rateLimitService.getCurrentMinuteCount(userId);
        long dayCountRedis = rateLimitService.getCurrentDayCount(userId);

        // Datos históricos desde PostgreSQL
        UsageSummary todaySummary = usageSummaryRepository
                .findByUserAndUsageDate(user, LocalDate.now())
                .orElse(UsageSummary.builder().totalRequests(0).totalTokens(0).build());

        int requestsToday = todaySummary.getTotalRequests();
        int tokensToday = todaySummary.getTotalTokens();

        double dailyPercent = plan.getMaxRequestsPerDay() > 0
                ? (requestsToday * 100.0) / plan.getMaxRequestsPerDay()
                : 0;
        double minutePercent = plan.getMaxRequestsPerMinute() > 0
                ? (minuteCount * 100.0) / plan.getMaxRequestsPerMinute()
                : 0;

        return UsageStatusDto.builder()
                .plan(plan.getName())
                .requestsToday(requestsToday)
                .maxRequestsPerDay(plan.getMaxRequestsPerDay())
                .requestsThisMinute((int) minuteCount)
                .maxRequestsPerMinute(plan.getMaxRequestsPerMinute())
                .totalTokensToday(tokensToday)
                .maxTokensPerRequest(plan.getMaxTokensPerRequest())
                .dailyUsagePercent(Math.min(dailyPercent, 100))
                .minuteUsagePercent(Math.min(minutePercent, 100))
                .rateLimited(requestsToday >= plan.getMaxRequestsPerDay()
                        || minuteCount >= plan.getMaxRequestsPerMinute())
                .resetInfo("Daily quota resets at midnight UTC")
                .build();
    }

    public Page<RequestLog> getRequestHistory(User user, Pageable pageable) {
        return requestLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<UsageSummary> getWeeklyUsage(User user) {
        LocalDate today = LocalDate.now();
        return usageSummaryRepository
                .findByUserAndUsageDateBetweenOrderByUsageDateAsc(user, today.minusDays(6), today);
    }
}
