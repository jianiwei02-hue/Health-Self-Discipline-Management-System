package com.health.health_system.repository;

import com.health.health_system.entity.ArticleCollect;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArticleCollectRepository extends JpaRepository<ArticleCollect, Integer> {

    Optional<ArticleCollect> findByUserIdAndArticleId(Integer userId, Integer articleId);

    boolean existsByUserIdAndArticleId(Integer userId, Integer articleId);

    void deleteByUserIdAndArticleId(Integer userId, Integer articleId);

    // 分页获取用户的收藏记录（按收藏时间倒序）
    List<ArticleCollect> findByUserIdOrderByCreateTimeDesc(Integer userId, Pageable pageable);

    // 统计用户的收藏总数
    int countByUserId(Integer userId);
}