package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "water_record")
public class WaterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "water_ml")
    private Integer waterMl;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}