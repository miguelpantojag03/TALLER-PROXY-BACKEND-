package com.aiplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiRequest {

    @NotBlank(message = "Prompt cannot be empty")
    @Size(max = 2000, message = "Prompt too long")
    private String prompt;
}
