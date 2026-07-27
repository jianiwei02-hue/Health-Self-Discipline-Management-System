package com.health.health_system.repository;

import com.health.health_system.entity.SleepRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SleepRecordRepository extends JpaRepository<SleepRecord, Integer> {

    Optional<SleepRecord> findByUserIdAndRecordDate(Integer userId, LocalDate recordDate);

    List<SleepRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
            Integer userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT AVG(s.sleepHours) FROM SleepRecord s " +
            "WHERE s.userId = :userId AND s.recordDate BETWEEN :startDate AND :endDate")
    Double getAvgSleepHours(@Param("userId") Integer userId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);
}