package com.aiplatform.repository;

import com.aiplatform.entity.RequestLog;
import com.aiplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    Page<RequestLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime after);

    List<RequestLog> findTop10ByUserOrderByCreatedAtDesc(User user);
}
