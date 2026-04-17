package com.aiplatform.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String limitType; // MINUTE or DAY

    public RateLimitExceededException(String limitType, String message) {
        super(message);
        this.limitType = limitType;
    }

    public String getLimitType() {
        return limitType;
    }
}
