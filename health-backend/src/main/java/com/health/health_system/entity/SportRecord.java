package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sport_record")
public class SportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "sport_name")
    private String sportName;

    private Integer duration;  // 时长(分钟)

    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}