package com.aiplatform.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UsageStatusDto {
    private String plan;
    private int requestsToday;
    private int maxRequestsPerDay;
    private int requestsThisMinute;
    private int maxRequestsPerMinute;
    private int totalTokensToday;
    private int maxTokensPerRequest;
    private double dailyUsagePercent;   // 0-100
    private double minuteUsagePercent;  // 0-100
    private boolean rateLimited;
    private String resetInfo;
}
