package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "post_comment")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_id")
    private Integer postId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(length = 300)
    private String content;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // ========== 回复功能字段 ==========
    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "reply_to_user_id")
    private Integer replyToUserId;

    @Column(name = "root_id")
    private Integer rootId;

    // ========== 置顶字段 ==========
    @Column(name = "is_pinned")
    private Integer isPinned = 0;  // 是否置顶: 0否, 1是

    // ========== 以下不是数据库字段，用于前端展示 ==========
    @Transient
    private String username;

    @Transient
    private String nickname;

    @Transient
    private String userAvatar;

    @Transient
    private String replyToUsername;

    @Transient
    private List<PostComment> replies;
}