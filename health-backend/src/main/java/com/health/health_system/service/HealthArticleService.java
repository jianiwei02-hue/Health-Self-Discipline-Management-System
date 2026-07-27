package com.health.health_system.service;

import com.health.health_system.entity.ArticleCollect;
import com.health.health_system.entity.HealthArticle;
import com.health.health_system.repository.ArticleCollectRepository;
import com.health.health_system.repository.HealthArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HealthArticleService {

    @Autowired
    private HealthArticleRepository healthArticleRepository;

    @Autowired
    private ArticleCollectRepository articleCollectRepository;

    // 获取所有已发布的文章
    public List<HealthArticle> getAllPublishedArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return healthArticleRepository.findByStatusOrderByCreateTimeDesc(1, pageable);
    }

    // 按分类获取文章
    public List<HealthArticle> getArticlesByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return healthArticleRepository.findByCategoryAndStatusOrderByCreateTimeDesc(category, 1, pageable);
    }

    // 获取热门文章
    public List<HealthArticle> getHotArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return healthArticleRepository.findByStatusOrderByViewCountDesc(1, pageable);
    }

    // 获取文章详情
    public HealthArticle getArticleDetail(Integer id) {
        Optional<HealthArticle> article = healthArticleRepository.findById(id);
        if (article.isPresent()) {
            HealthArticle a = article.get();
            // 增加浏览量
            a.setViewCount((a.getViewCount() == null ? 0 : a.getViewCount()) + 1);
            return healthArticleRepository.save(a);
        }
        return null;
    }

    // 点赞文章
    public HealthArticle likeArticle(Integer id) {
        Optional<HealthArticle> article = healthArticleRepository.findById(id);
        if (article.isPresent()) {
            HealthArticle a = article.get();
            a.setLikeCount((a.getLikeCount() == null ? 0 : a.getLikeCount()) + 1);
            return healthArticleRepository.save(a);
        }
        return null;
    }

    // ========== 收藏功能 ==========

    // 收藏文章
    @Transactional
    public boolean collectArticle(Integer userId, Integer articleId) {
        // 检查文章是否存在
        if (!healthArticleRepository.existsById(articleId)) {
            return false;
        }
        // 检查是否已经收藏
        if (articleCollectRepository.existsByUserIdAndArticleId(userId, articleId)) {
            return true; // 已经收藏过了
        }
        ArticleCollect collect = new ArticleCollect();
        collect.setUserId(userId);
        collect.setArticleId(articleId);
        collect.setCreateTime(LocalDateTime.now());
        articleCollectRepository.save(collect);
        return true;
    }

    // 取消收藏
    @Transactional
    public boolean uncollectArticle(Integer userId, Integer articleId) {
        articleCollectRepository.deleteByUserIdAndArticleId(userId, articleId);
        return true;
    }

    // 查询是否已收藏
    public boolean isCollected(Integer userId, Integer articleId) {
        if (userId == null) return false;
        return articleCollectRepository.existsByUserIdAndArticleId(userId, articleId);
    }

    // 获取相关文章（同分类的其他文章，排除当前文章）
    public List<HealthArticle> getRelatedArticles(Integer articleId, String category, Integer limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return healthArticleRepository.findRelatedArticles(articleId, category, pageable);
    }

    // ========== 新增：获取用户收藏的文章列表 ==========

    // 获取用户收藏的文章列表（分页）
    public List<HealthArticle> getUserFavorites(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // 1. 查询用户收藏的记录（获取文章ID列表）
        List<ArticleCollect> collects = articleCollectRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        if (collects == null || collects.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 根据文章ID列表查询文章详情
        List<Integer> articleIds = collects.stream()
                .map(ArticleCollect::getArticleId)
                .collect(Collectors.toList());
        return healthArticleRepository.findAllById(articleIds);
    }

    // 获取用户收藏文章的总数量
    public int getUserFavoritesCount(Integer userId) {
        return articleCollectRepository.countByUserId(userId);
    }

    // ========== 管理员功能 ==========

    // 管理员：发布文章
    public HealthArticle createArticle(HealthArticle article) {
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setStatus(1);
        return healthArticleRepository.save(article);
    }

    // 管理员：删除文章
    public void deleteArticle(Integer id) {
        healthArticleRepository.deleteById(id);
    }
}