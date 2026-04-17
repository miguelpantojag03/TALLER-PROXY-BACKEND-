package com.aiplatform.controller;

import com.aiplatform.dto.UsageStatusDto;
import com.aiplatform.entity.RequestLog;
import com.aiplatform.entity.UsageSummary;
import com.aiplatform.entity.User;
import com.aiplatform.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    /**
     * GET /api/usage/status
     * Estado actual de consumo del usuario autenticado.
     */
    @GetMapping("/status")
    public ResponseEntity<UsageStatusDto> getStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(usageService.getUsageStatus(user));
    }

    /**
     * GET /api/usage/history?page=0&size=10
     * Historial paginado de requests.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<RequestLog>> getHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                usageService.getRequestHistory(user, PageRequest.of(page, Math.min(size, 50)))
        );
    }

    /**
     * GET /api/usage/weekly
     * Resumen de los últimos 7 días para el gráfico del dashboard.
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<UsageSummary>> getWeekly(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(usageService.getWeeklyUsage(user));
    }

    /**
     * GET /api/usage/stream
     * Server-Sent Events — el frontend se suscribe y recibe actualizaciones cada 5 segundos.
     * Permite mostrar el estado de consumo en tiempo real sin polling manual.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUsage(@AuthenticationPrincipal User user) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutos de timeout

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i < 60; i++) { // máximo 60 eventos (5 min)
                    UsageStatusDto status = usageService.getUsageStatus(user);
                    emitter.send(SseEmitter.event()
                            .name("usage-update")
                            .data(status));
                    Thread.sleep(5000); // cada 5 segundos
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
