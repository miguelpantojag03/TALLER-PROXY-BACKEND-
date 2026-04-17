package com.aiplatform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // FREE, PRO, ENTERPRISE

    @Column(nullable = false)
    private int maxRequestsPerMinute;

    @Column(nullable = false)
    private int maxRequestsPerDay;

    @Column(nullable = false)
    private int maxTokensPerRequest;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal priceUsd;
}
