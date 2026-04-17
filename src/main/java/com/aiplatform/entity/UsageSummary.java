package com.aiplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "usage_summary",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "usage_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    @Builder.Default
    private int totalRequests = 0;

    @Column(nullable = false)
    @Builder.Default
    private int totalTokens = 0;
}
