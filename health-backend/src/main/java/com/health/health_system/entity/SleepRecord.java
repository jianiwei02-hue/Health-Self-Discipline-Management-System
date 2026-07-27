package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sleep_record")
public class SleepRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "sleep_hours")
    private Double sleepHours;

    @Column(name = "deep_sleep_hours")
    private Double deepSleepHours;

    private String quality;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}