package com.health.health_system.controller;

import com.health.health_system.entity.HealthArticle;
import com.health.health_system.service.HealthArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health-article")
@CrossOrigin(origins = "*")
public class HealthArticleController {

    @Autowired
    private HealthArticleService healthArticleService;

    // 获取文章列表
    @GetMapping("/list")
    public Map<String, Object> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> result = new HashMap<>();
        List<HealthArticle> articles;

        if (category != null && !category.isEmpty()) {
            articles = healthArticleService.getArticlesByCategory(category, page, size);
        } else {
            articles = healthArticleService.getAllPublishedArticles(page, size);
        }

        result.put("success", true);
        result.put("articles", articles);
        return result;
    }

    // 获取热门文章
    @GetMapping("/hot")
    public Map<String, Object> getHotArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Map<String, Object> result = new HashMap<>();
        List<HealthArticle> articles = healthArticleService.getHotArticles(page, size);

        result.put("success", true);
        result.put("articles", articles);
        return result;
    }

    // 获取文章详情
    @GetMapping("/detail/{id}")
    public Map<String, Object> getArticleDetail(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        HealthArticle article = healthArticleService.getArticleDetail(id);

        if (article != null) {
            result.put("success", true);
            result.put("article", article);
        } else {
            result.put("success", false);
            result.put("message", "文章不存在");
        }
        return result;
    }

    // 点赞文章
    @PostMapping("/like/{id}")
    public Map<String, Object> likeArticle(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        HealthArticle article = healthArticleService.likeArticle(id);

        if (article != null) {
            result.put("success", true);
            result.put("likeCount", article.getLikeCount());
        } else {
            result.put("success", false);
            result.put("message", "文章不存在");
        }
        return result;
    }

    // ========== 收藏功能 ==========

    // 收藏文章
    @PostMapping("/collect/{id}")
    public Map<String, Object> collectArticle(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            healthArticleService.collectArticle(userId, id);
            result.put("success", true);
            result.put("message", "收藏成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 取消收藏
    @DeleteMapping("/collect/{id}")
    public Map<String, Object> uncollectArticle(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            healthArticleService.uncollectArticle(userId, id);
            result.put("success", true);
            result.put("message", "已取消收藏");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 查询收藏状态
    @GetMapping("/collect-status/{id}")
    public Map<String, Object> getCollectStatus(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        boolean isCollected = healthArticleService.isCollected(userId, id);
        result.put("success", true);
        result.put("isCollected", isCollected);
        return result;
    }

    // ========== 新增：获取用户收藏列表 ==========

    // 获取用户收藏的文章列表
    @GetMapping("/favorites/{userId}")
    public Map<String, Object> getUserFavorites(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> result = new HashMap<>();
        List<HealthArticle> articles = healthArticleService.getUserFavorites(userId, page, size);
        int total = healthArticleService.getUserFavoritesCount(userId);

        result.put("success", true);
        result.put("articles", articles);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    // 获取相关文章推荐
    @GetMapping("/related/{id}")
    public Map<String, Object> getRelatedArticles(
            @PathVariable Integer id,
            @RequestParam String category,
            @RequestParam(defaultValue = "5") int limit) {
        Map<String, Object> result = new HashMap<>();
        List<HealthArticle> articles = healthArticleService.getRelatedArticles(id, category, limit);
        result.put("success", true);
        result.put("data", articles);
        return result;
    }
}