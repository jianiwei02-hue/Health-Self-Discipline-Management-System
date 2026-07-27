package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String images;

    private String tag;

    @Column(name = "sync_health_data")
    private Integer syncHealthData;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // ========== 新增字段 ==========
    @Column(name = "is_pinned")
    private Integer isPinned = 0;  // 0:否, 1:置顶

    @Column(name = "is_featured")
    private Integer isFeatured = 0;  // 0:否, 1:加精
}