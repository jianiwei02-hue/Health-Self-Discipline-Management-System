package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "diet_record")
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "meal_type")
    private String mealType;  // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(name = "food_name")
    private String foodName;

    private Integer quantity;  // 数量(g)

    private Integer calories;  // 总热量

    // ========== 新增 7 个营养字段 ==========
    @Column(name = "protein", columnDefinition = "DECIMAL(10,1) DEFAULT 0")
    private Double protein;  // 蛋白质(g)

    @Column(name = "fat", columnDefinition = "DECIMAL(10,1) DEFAULT 0")
    private Double fat;  // 脂肪(g)

    @Column(name = "carbs", columnDefinition = "DECIMAL(10,1) DEFAULT 0")
    private Double carbs;  // 碳水化合物(g)

    @Column(name = "fiber", columnDefinition = "DECIMAL(10,1) DEFAULT 0")
    private Double fiber;  // 膳食纤维(g)

    @Column(name = "sugar", columnDefinition = "DECIMAL(10,1) DEFAULT 0")
    private Double sugar;  // 糖(g)

    @Column(name = "sodium")
    private Integer sodium;  // 钠(mg)

    @Column(name = "calcium")
    private Integer calcium;  // 钙(mg)

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}