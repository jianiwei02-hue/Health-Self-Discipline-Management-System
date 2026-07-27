package com.health.health_system.repository;

import com.health.health_system.entity.HealthWeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthWeeklyReportRepository extends JpaRepository<HealthWeeklyReport, Integer> {

    Optional<HealthWeeklyReport> findByUserIdAndWeekStartDate(Integer userId, LocalDate weekStartDate);

    List<HealthWeeklyReport> findByUserIdOrderByWeekStartDateDesc(Integer userId);

    List<HealthWeeklyReport> findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateAsc(
            Integer userId, LocalDate startDate, LocalDate endDate);
}