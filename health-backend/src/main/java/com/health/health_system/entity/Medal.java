package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medal")
public class Medal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "medal_name")
    private String medalName;

    @Column(name = "medal_icon")
    private String medalIcon;

    @Column(name = "get_time")
    private LocalDateTime getTime;
}