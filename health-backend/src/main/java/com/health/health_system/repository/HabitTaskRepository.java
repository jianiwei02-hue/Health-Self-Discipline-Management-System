package com.health.health_system.repository;

import com.health.health_system.entity.HabitTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface HabitTaskRepository extends JpaRepository<HabitTask, Integer> {

    // 查询用户某一天的所有任务
    List<HabitTask> findByUserIdAndRecordDate(Integer userId, LocalDate recordDate);

    // 查询用户某一天指定状态的任务
    List<HabitTask> findByUserIdAndRecordDateAndStatus(Integer userId, LocalDate recordDate, Integer status);

    // 删除用户某一天的所有任务（用于重置）
    void deleteByUserIdAndRecordDate(Integer userId, LocalDate recordDate);

    // 获取用户所有已完成的任务（按 recordDate 倒序）
    List<HabitTask> findByUserIdAndStatusOrderByRecordDateDesc(Integer userId, Integer status);

    // 获取用户所有已完成的任务（不分日期）
    List<HabitTask> findByUserIdAndStatus(Integer userId, Integer status);

    // ========== 新增方法（用于勋章统计） ==========

    // 查询用户日期范围内的所有任务
    List<HabitTask> findByUserIdAndRecordDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);
}