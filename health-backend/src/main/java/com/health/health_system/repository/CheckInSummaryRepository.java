package com.health.health_system.repository;

import com.health.health_system.entity.CheckInSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CheckInSummaryRepository extends JpaRepository<CheckInSummary, Integer> {

    // 根据用户ID查询打卡汇总
    Optional<CheckInSummary> findByUserId(Integer userId);
}