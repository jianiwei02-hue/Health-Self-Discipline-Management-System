package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "health_weekly_report")
public class HealthWeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Column(name = "avg_weight")
    private Double avgWeight;

    @Column(name = "avg_bmi")
    private Double avgBmi;

    @Column(name = "avg_body_fat")
    private Double avgBodyFat;

    @Column(name = "avg_sleep")
    private Double avgSleep;

    @Column(name = "total_water")
    private Integer totalWater;

    @Column(name = "sport_count")
    private Integer sportCount;

    @Column(name = "weight_change")
    private Double weightChange;

    private String advice;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}