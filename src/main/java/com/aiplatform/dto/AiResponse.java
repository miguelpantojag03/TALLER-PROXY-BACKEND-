package com.aiplatform.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AiResponse {
    private String generatedText;
    private int tokensUsed;
    private UsageStatusDto usageStatus;
}
