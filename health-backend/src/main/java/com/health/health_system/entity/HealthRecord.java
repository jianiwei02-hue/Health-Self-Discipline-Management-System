package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "health_record")
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    private Double height;

    private Double weight;

    private Double bmi;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "body_fat")
    private Double bodyFat;

    @Column(name = "chest")
    private Double chest;

    @Column(name = "waist")
    private Double waist;

    @Column(name = "hip")
    private Double hip;

    @Column(name = "thigh")
    private Double thigh;

    @Column(name = "resting_heart_rate")
    private Integer restingHeartRate;

    @Column(name = "sleep_duration")
    private Double sleepDuration;

    @Column(name = "water_intake")
    private Integer waterIntake;
}