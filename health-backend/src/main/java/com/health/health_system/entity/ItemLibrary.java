package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "item_library")
public class ItemLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String type;  // FOOD 或 SPORT

    private String name;

    private String unit;

    private Integer calories;

    private String remark;

    // 新增：运动代谢当量 MET 值（用于精确计算热量）
    @Column(name = "met_value")
    private Double metValue;

    // 新增：运动强度等级（LOW/MEDIUM/HIGH）
    @Column(name = "intensity")
    private String intensity;
}