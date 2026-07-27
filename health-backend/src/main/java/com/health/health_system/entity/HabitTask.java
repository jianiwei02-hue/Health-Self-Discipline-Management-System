package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "habit_task")
public class HabitTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "target_value")
    private String targetValue;

    private Integer status;  // 0-未完成, 1-已完成

    @Column(name = "record_date")
    private LocalDate recordDate;
}