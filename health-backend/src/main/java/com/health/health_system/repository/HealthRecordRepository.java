package com.health.health_system.repository;

import com.health.health_system.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Integer> {

    List<HealthRecord> findByUserIdOrderByRecordDateAsc(Integer userId);

    Optional<HealthRecord> findByUserIdAndRecordDate(Integer userId, LocalDate recordDate);

    List<HealthRecord> findByUserIdAndRecordDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);
}