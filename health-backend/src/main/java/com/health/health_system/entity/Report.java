package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "report")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id")
    private Integer reporterId;

    @Column(name = "target_type")
    private String targetType;  // POST, COMMENT

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_user_id")
    private Integer targetUserId;  // 被举报人ID（新增字段）

    private String reason;

    private Integer status;  // 0:待处理, 1:已处理, 2:已驳回

    @Column(name = "handler_id")
    private Integer handlerId;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "handle_note")
    private String handleNote;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // ========== 非数据库字段（用于前端展示） ==========
    @Transient
    private String reporterName;  // 举报人用户名

    @Transient
    private String targetContent;  // 被举报的内容
}