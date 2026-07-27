package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "check_in_summary")
public class CheckInSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "last_check_in_date")
    private LocalDate lastCheckInDate;

    @Column(name = "consecutive_days")
    private Integer consecutiveDays;

    @Column(name = "total_days")
    private Integer totalDays;
}