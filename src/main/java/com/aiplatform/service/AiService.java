package com.aiplatform.service;

import com.aiplatform.dto.AiRequest;
import com.aiplatform.dto.AiResponse;
import com.aiplatform.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Simula un servicio de generación de texto con IA.
 * En producción real, aquí se llamaría a OpenAI, Anthropic, etc.
 *
 * El flujo es:
 * 1. RateLimitService verifica y consume cuota (Redis)
 * 2. AiService genera la respuesta simulada
 * 3. UsageService registra el consumo (PostgreSQL)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final RateLimitService rateLimitService;
    private final UsageService usageService;

    private static final List<String> RESPONSE_TEMPLATES = List.of(
            "Based on your prompt, here is a thoughtful analysis: %s is a fascinating topic that involves multiple dimensions of consideration. The key aspects to understand are the underlying principles, practical applications, and potential implications for the future.",
            "Great question! Regarding '%s': This subject has been extensively studied and the consensus points to several important factors. First, we must consider the foundational elements. Second, the contextual variables play a crucial role. Finally, the synthesis of these elements leads to actionable insights.",
            "Analyzing your request about '%s': The data suggests a multi-faceted approach is optimal. Consider the following framework: (1) Define the core objective, (2) Identify constraints and opportunities, (3) Evaluate alternatives systematically, (4) Implement with iterative feedback loops.",
            "In response to your query on '%s': Modern research highlights three critical dimensions. The theoretical framework provides the foundation, empirical evidence validates the approach, and practical implementation bridges the gap between concept and reality.",
            "Your prompt about '%s' touches on an important area. Let me provide a structured response: The primary consideration is establishing clear parameters. Subsequently, applying systematic analysis reveals patterns that inform optimal decision-making strategies."
    );

    private final Random random = new Random();

    /**
     * Procesa una solicitud de IA aplicando rate limiting y registrando consumo.
     */
    public AiResponse process(User user, AiRequest request) {
        // 1. Verificar rate limit (lanza excepción si se supera)
        rateLimitService.checkAndConsume(user);

        // 2. Generar respuesta simulada
        String generatedText = generateText(request.getPrompt(), user.getPlan().getMaxTokensPerRequest());
        int tokensUsed = estimateTokens(request.getPrompt(), generatedText);

        // 3. Registrar consumo exitoso
        usageService.recordSuccess(user, request.getPrompt(), generatedText, tokensUsed);

        // 4. Obtener estado actualizado de consumo
        var usageStatus = usageService.getUsageStatus(user);

        log.info("AI request processed for user={}, tokens={}, plan={}",
                user.getUsername(), tokensUsed, user.getPlan().getName());

        return AiResponse.builder()
                .generatedText(generatedText)
                .tokensUsed(tokensUsed)
                .usageStatus(usageStatus)
                .build();
    }

    private String generateText(String prompt, int maxTokens) {
        // Simula latencia de procesamiento (50-300ms)
        try {
            Thread.sleep(50 + random.nextInt(250));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String template = RESPONSE_TEMPLATES.get(random.nextInt(RESPONSE_TEMPLATES.size()));
        String response = String.format(template, truncatePrompt(prompt, 50));

        // Respetar límite de tokens del plan (aprox 4 chars por token)
        int maxChars = maxTokens * 4;
        return response.length() > maxChars ? response.substring(0, maxChars) + "..." : response;
    }

    private String truncatePrompt(String prompt, int maxLen) {
        return prompt.length() > maxLen ? prompt.substring(0, maxLen) + "..." : prompt;
    }

    /**
     * Estimación simple: ~1 token por cada 4 caracteres (similar a GPT tokenization).
     */
    private int estimateTokens(String prompt, String response) {
        return (prompt.length() + response.length()) / 4;
    }
}
