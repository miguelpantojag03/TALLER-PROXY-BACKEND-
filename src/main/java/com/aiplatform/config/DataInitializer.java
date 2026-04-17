package com.aiplatform.config;

import com.aiplatform.entity.Plan;
import com.aiplatform.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inicializa los planes en la base de datos al arrancar la aplicación.
 * Solo inserta si no existen (idempotente).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        if (planRepository.count() == 0) {
            List<Plan> plans = List.of(
                Plan.builder()
                    .name("FREE")
                    .maxRequestsPerMinute(5)
                    .maxRequestsPerDay(50)
                    .maxTokensPerRequest(500)
                    .priceUsd(BigDecimal.ZERO)
                    .build(),
                Plan.builder()
                    .name("PRO")
                    .maxRequestsPerMinute(30)
                    .maxRequestsPerDay(1000)
                    .maxTokensPerRequest(2000)
                    .priceUsd(new BigDecimal("29.99"))
                    .build(),
                Plan.builder()
                    .name("ENTERPRISE")
                    .maxRequestsPerMinute(100)
                    .maxRequestsPerDay(10000)
                    .maxTokensPerRequest(8000)
                    .priceUsd(new BigDecimal("199.99"))
                    .build()
            );
            planRepository.saveAll(plans);
            log.info("Plans initialized: FREE, PRO, ENTERPRISE");
        }
    }
}
