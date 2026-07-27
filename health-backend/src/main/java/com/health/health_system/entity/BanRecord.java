package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ban_record")
public class BanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "admin_id")
    private Integer adminId;

    @Column(name = "ban_days")
    private Integer banDays;

    private String reason;

    @Column(name = "ban_start_time")
    private LocalDateTime banStartTime;

    @Column(name = "ban_end_time")
    private LocalDateTime banEndTime;

    @Column(name = "is_active")
    private Integer isActive = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}