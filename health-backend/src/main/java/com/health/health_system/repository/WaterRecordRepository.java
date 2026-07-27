package com.health.health_system.repository;

import com.health.health_system.entity.WaterRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterRecordRepository extends JpaRepository<WaterRecord, Integer> {

    Optional<WaterRecord> findByUserIdAndRecordDate(Integer userId, LocalDate recordDate);

    List<WaterRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
            Integer userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(w.waterMl) FROM WaterRecord w " +
            "WHERE w.userId = :userId AND w.recordDate BETWEEN :startDate AND :endDate")
    Integer getTotalWater(@Param("userId") Integer userId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);
}