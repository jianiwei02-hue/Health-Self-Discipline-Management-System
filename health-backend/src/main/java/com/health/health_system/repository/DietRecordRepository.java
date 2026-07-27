package com.health.health_system.repository;

import com.health.health_system.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DietRecordRepository extends JpaRepository<DietRecord, Integer> {

    // 查询用户某一天的所有饮食记录
    List<DietRecord> findByUserIdAndRecordDateOrderByMealType(Integer userId, LocalDate recordDate);

    List<DietRecord> findByUserIdOrderByRecordDateDesc(Integer userId);

    // 查询用户某一天某一餐的记录
    List<DietRecord> findByUserIdAndRecordDateAndMealType(Integer userId, LocalDate recordDate, String mealType);

    // 查询用户某一天的总热量
    @Query("SELECT COALESCE(SUM(d.calories), 0) FROM DietRecord d WHERE d.userId = :userId AND d.recordDate = :recordDate")
    Integer sumCaloriesByUserIdAndRecordDate(@Param("userId") Integer userId, @Param("recordDate") LocalDate recordDate);

    // ========== 新增方法 ==========

    // 查询用户日期范围内的饮食记录
    List<DietRecord> findByUserIdAndRecordDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);
}