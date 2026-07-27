package com.health.health_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_follow")
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "follower_id")
    private Integer followerId;

    @Column(name = "followed_id")
    private Integer followedId;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}