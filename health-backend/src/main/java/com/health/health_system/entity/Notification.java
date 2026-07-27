package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "from_user_id")
    private Integer fromUserId;

    private String type;  // LIKE, COMMENT, REPLY, MENTION

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "target_type")
    private String targetType;  // POST, COMMENT

    private String content;

    @Column(name = "is_read")
    private Integer isRead = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // ========== 新增字段：用于跳转到具体帖子 ==========
    @Column(name = "post_id")
    private Long postId;           // 关联的帖子ID

    @Column(name = "comment_id")
    private Long commentId;        // 关联的评论ID（可选，用于精准定位）

    // ========== 非数据库字段 ==========
    @Transient
    private String fromUsername;

    @Transient
    private String fromUserAvatar;

    // ========== 手动添加 getter/setter（防止 Lombok 不生效） ==========
    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }
}