package com.health.health_system.repository;

import com.health.health_system.entity.HealthArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HealthArticleRepository extends JpaRepository<HealthArticle, Integer> {

    // 获取已发布的文章（按时间倒序）
    List<HealthArticle> findByStatusOrderByCreateTimeDesc(Integer status, Pageable pageable);

    // 按分类获取文章
    List<HealthArticle> findByCategoryAndStatusOrderByCreateTimeDesc(String category, Integer status, Pageable pageable);

    // 获取热门文章（按浏览量）
    List<HealthArticle> findByStatusOrderByViewCountDesc(Integer status, Pageable pageable);

    // 相关文章推荐（同分类，排除当前文章）
    @Query("SELECT a FROM HealthArticle a WHERE a.category = :category AND a.id != :articleId AND a.status = 1 ORDER BY a.createTime DESC")
    List<HealthArticle> findRelatedArticles(@Param("articleId") Integer articleId, @Param("category") String category, Pageable pageable);

    // 关键词搜索（分页）
    @Query("SELECT a FROM HealthArticle a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword% ORDER BY a.createTime DESC")
    Page<HealthArticle> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}