package com.aiplatform.repository;

import com.aiplatform.entity.UsageSummary;
import com.aiplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageSummaryRepository extends JpaRepository<UsageSummary, Long> {

    Optional<UsageSummary> findByUserAndUsageDate(User user, LocalDate date);

    List<UsageSummary> findByUserOrderByUsageDateDesc(User user);

    List<UsageSummary> findByUserAndUsageDateBetweenOrderByUsageDateAsc(
            User user, LocalDate from, LocalDate to);
}
