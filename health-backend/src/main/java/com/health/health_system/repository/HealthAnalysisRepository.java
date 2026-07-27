package com.health.health_system.repository;

import com.health.health_system.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthAnalysisRepository extends JpaRepository<HealthRecord, Integer> {

    // 查询某个用户的所有健康记录（按日期排序）
    List<HealthRecord> findByUserIdOrderByRecordDateAsc(Integer userId);

    // 查询某个用户某一天的记录
    Optional<HealthRecord> findByUserIdAndRecordDate(Integer userId, LocalDate recordDate);

    List<HealthRecord> findByUserIdAndRecordDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);
}
