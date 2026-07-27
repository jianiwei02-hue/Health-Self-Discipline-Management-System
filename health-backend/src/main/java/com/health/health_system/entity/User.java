package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;

    private String role;

    private Integer age;

    private String gender;

    @Column(name = "current_height")
    private Double currentHeight;

    @Column(name = "current_weight")
    private Double currentWeight;

    @Column(name = "target_weight")
    private Double targetWeight;

    @Column(name = "target_body_fat")
    private Double targetBodyFat;

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

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "avatar")
    private String avatar;

    @Column(unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    private String openId;

    // ========== 活动量字段 ==========
    @Column(name = "activity_level")
    private String activityLevel;  // SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE

    // ========== 统计字段 ==========
    @Column(name = "follow_count")
    private Integer followCount = 0;

    @Column(name = "fans_count")
    private Integer fansCount = 0;

    @Column(name = "mutual_count")
    private Integer mutualCount = 0;

    // ========== 禁言相关字段 ==========
    @Column(name = "is_banned")
    private Integer isBanned = 0;  // 0:正常, 1:禁言中

    @Column(name = "ban_end_time")
    private LocalDateTime banEndTime;  // 禁言结束时间

    // ========== 账号状态字段 ==========
    @Column(name = "status")
    private Integer status = 1;  // 1:正常, 0:禁用
}