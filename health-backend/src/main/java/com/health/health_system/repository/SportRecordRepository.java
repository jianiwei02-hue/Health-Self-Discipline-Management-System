package com.health.health_system.repository;

import com.health.health_system.entity.SportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface SportRecordRepository extends JpaRepository<SportRecord, Integer> {

    List<SportRecord> findByUserIdAndRecordDateOrderByIdDesc(Integer userId, LocalDate recordDate);

    List<SportRecord> findByUserIdOrderByRecordDateDesc(Integer userId);

    // 查询时间段内的运动记录
    List<SportRecord> findByUserIdAndRecordDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(s.caloriesBurned) FROM SportRecord s WHERE s.userId = :userId AND s.recordDate = :recordDate")
    Integer sumCaloriesByUserIdAndRecordDate(Integer userId, LocalDate recordDate);
}