package com.health.health_system.controller;

import com.health.health_system.entity.Post;
import com.health.health_system.entity.PostComment;
import com.health.health_system.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@CrossOrigin(origins = "*")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    // 发布动态
    @PostMapping("/post")
    public Map<String, Object> createPost(
            @RequestParam Integer userId,
            @RequestParam String content,
            @RequestParam(required = false) String images,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer syncHealthData) {

        Map<String, Object> result = new HashMap<>();
        Post post = communityService.createPost(userId, content, images, tag, syncHealthData);
        result.put("success", true);
        result.put("message", "发布成功");
        result.put("post", post);
        return result;
    }

    // 获取动态列表
    @GetMapping("/posts")
    public List<Map<String, Object>> getPosts(
            @RequestParam Integer currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return communityService.getAllPosts(currentUserId, page, size);
    }

    // 获取热门动态
    @GetMapping("/hot-posts")
    public List<Map<String, Object>> getHotPosts(
            @RequestParam Integer currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return communityService.getHotPosts(currentUserId, page, size);
    }

    // 获取用户动态
    @GetMapping("/user-posts/{userId}")
    public List<Map<String, Object>> getUserPosts(
            @PathVariable Integer userId,
            @RequestParam(required = false) Integer currentUserId) {
        return communityService.getUserPosts(userId, currentUserId);
    }

    // 点赞
    @PostMapping("/like")
    public Map<String, Object> like(
            @RequestParam Integer postId,
            @RequestParam Integer userId) {
        return communityService.like(postId, userId);
    }

    // 取消点赞
    @DeleteMapping("/like")
    public Map<String, Object> unlike(
            @RequestParam Integer postId,
            @RequestParam Integer userId) {
        return communityService.unlike(postId, userId);
    }

    // 删除动态
    @DeleteMapping("/post/{postId}")
    public Map<String, Object> deletePost(
            @PathVariable Integer postId,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        communityService.deletePost(postId, userId);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // 添加顶级评论
    @PostMapping("/comment")
    public Map<String, Object> addComment(
            @RequestParam Integer postId,
            @RequestParam Integer userId,
            @RequestParam String content) {

        Map<String, Object> result = new HashMap<>();
        PostComment comment = communityService.addComment(postId, userId, content);
        result.put("success", true);
        result.put("message", "评论成功");
        result.put("comment", comment);
        return result;
    }

    // 获取评论列表（简单版）
    @GetMapping("/comments/{postId}")
    public List<Map<String, Object>> getComments(@PathVariable Integer postId) {
        return communityService.getComments(postId);
    }

    // 删除评论
    @DeleteMapping("/comment/{commentId}")
    public Map<String, Object> deleteComment(
            @PathVariable Integer commentId,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        communityService.deleteComment(commentId, userId);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // 回复评论
    @PostMapping("/reply")
    public Map<String, Object> replyComment(
            @RequestParam Integer postId,
            @RequestParam Integer userId,
            @RequestParam String content,
            @RequestParam Integer parentId,
            @RequestParam(required = false) Integer replyToUserId) {

        Map<String, Object> result = new HashMap<>();
        PostComment reply = communityService.replyComment(postId, userId, content, parentId, replyToUserId);
        result.put("success", true);
        result.put("message", "回复成功");
        result.put("reply", reply);
        return result;
    }

    // 获取带回复的评论列表
    @GetMapping("/comments-with-replies/{postId}")
    public List<Map<String, Object>> getCommentsWithReplies(@PathVariable Integer postId) {
        return communityService.getCommentsWithReplies(postId);
    }

    // 删除回复
    @DeleteMapping("/reply/{replyId}")
    public Map<String, Object> deleteReply(
            @PathVariable Integer replyId,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        communityService.deleteReply(replyId, userId);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // 评论点赞
    @PostMapping("/comment-like")
    public Map<String, Object> likeComment(
            @RequestParam Integer commentId,
            @RequestParam Integer userId) {
        return communityService.likeComment(commentId, userId);
    }

    // 取消评论点赞
    @DeleteMapping("/comment-like")
    public Map<String, Object> unlikeComment(
            @RequestParam Integer commentId,
            @RequestParam Integer userId) {
        return communityService.unlikeComment(commentId, userId);
    }

    // 置顶评论
    @PostMapping("/pin-comment")
    public Map<String, Object> pinComment(
            @RequestParam Integer commentId,
            @RequestParam Integer userId) {
        return communityService.pinComment(commentId, userId);
    }

    // 获取通知列表
    @GetMapping("/notifications")
    public Map<String, Object> getNotifications(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return communityService.getNotifications(userId, page, size);
    }

    // 标记通知为已读
    @PutMapping("/notification/{notificationId}/read")
    public Map<String, Object> markNotificationAsRead(
            @PathVariable Integer notificationId,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        communityService.markNotificationAsRead(notificationId, userId);
        result.put("success", true);
        return result;
    }

    // 标记所有通知为已读
    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllNotificationsAsRead(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        communityService.markAllNotificationsAsRead(userId);
        result.put("success", true);
        return result;
    }

    // 获取未读通知数量
    @GetMapping("/notifications/unread-count")
    public Map<String, Object> getUnreadNotificationCount(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("count", communityService.getUnreadNotificationCount(userId));
        return result;
    }

    // 上传图片
    @PostMapping("/upload-image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        String imageUrl = communityService.uploadImage(file);
        result.put("success", true);
        result.put("url", imageUrl);
        return result;
    }

    // ========== 获取评论点赞状态 ==========
    @GetMapping("/comment-like-status")
    public Map<String, Object> getCommentLikeStatus(
            @RequestParam Integer commentId,
            @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        boolean liked = communityService.isCommentLiked(commentId, userId);
        result.put("liked", liked);
        return result;
    }
}