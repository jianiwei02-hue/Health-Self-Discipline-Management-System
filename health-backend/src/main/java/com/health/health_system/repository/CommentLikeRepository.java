package com.health.health_system.repository;

import com.health.health_system.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Integer> {

    boolean existsByCommentIdAndUserId(Integer commentId, Integer userId);

    @Transactional
    @Modifying
    void deleteByCommentIdAndUserId(Integer commentId, Integer userId);

    int countByCommentId(Integer commentId);
}