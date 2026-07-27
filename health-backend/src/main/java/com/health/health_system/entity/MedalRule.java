package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "medal_rule")
public class MedalRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String medalName;

    private String medalIcon;

    private String conditionType;

    private Integer conditionValue;

    private String description;
}