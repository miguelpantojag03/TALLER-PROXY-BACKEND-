package com.aiplatform.controller;

import com.aiplatform.dto.AiRequest;
import com.aiplatform.dto.AiResponse;
import com.aiplatform.entity.User;
import com.aiplatform.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * POST /api/ai/generate
     * Endpoint principal — pasa por el proxy de rate limiting.
     */
    @PostMapping("/generate")
    public ResponseEntity<AiResponse> generate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AiRequest request) {
        return ResponseEntity.ok(aiService.process(user, request));
    }
}
