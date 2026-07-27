package com.health.health_system.repository;

import com.health.health_system.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

    // 检查是否已点赞
    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    // 删除单个点赞（用户取消点赞时调用）
    @Transactional
    @Modifying
    void deleteByPostIdAndUserId(Integer postId, Integer userId);

    // 删除帖子的所有点赞（删除帖子时调用）
    @Transactional
    @Modifying
    void deleteByPostId(Integer postId);

    // 统计帖子的点赞数
    int countByPostId(Integer postId);
}