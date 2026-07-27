package com.health.health_system.service;

import com.health.health_system.entity.*;
import com.health.health_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommunityService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SensitiveWordFilterService sensitiveWordFilterService;

    // ========== 禁言校验方法 ==========

    /**
     * 检查用户是否被禁言
     * @param userId 用户ID
     * @return true-已禁言，false-正常
     */
    public boolean isUserBanned(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getIsBanned() != null && user.getIsBanned() == 1) {
                // 检查禁言是否已过期
                LocalDateTime banEndTime = user.getBanEndTime();
                if (banEndTime != null && banEndTime.isAfter(LocalDateTime.now())) {
                    return true; // 仍在禁言期内
                } else if (banEndTime != null && banEndTime.isBefore(LocalDateTime.now())) {
                    // 禁言已过期，自动解除
                    user.setIsBanned(0);
                    user.setBanEndTime(null);
                    userRepository.save(user);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 获取禁言剩余时间（格式化）
     */
    public String getBanRemainingTime(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getIsBanned() != null && user.getIsBanned() == 1 && user.getBanEndTime() != null) {
                LocalDateTime end = user.getBanEndTime();
                LocalDateTime now = LocalDateTime.now();
                if (end.isAfter(now)) {
                    long days = java.time.Duration.between(now, end).toDays();
                    long hours = java.time.Duration.between(now, end).toHours() % 24;
                    long minutes = java.time.Duration.between(now, end).toMinutes() % 60;
                    if (days > 0) return days + "天" + hours + "小时";
                    if (hours > 0) return hours + "小时" + minutes + "分钟";
                    return minutes + "分钟";
                }
            }
        }
        return null;
    }

    /**
     * 校验用户是否被禁言，如果被禁言则抛出异常
     */
    private void checkUserBanStatus(Integer userId) {
        if (isUserBanned(userId)) {
            String remaining = getBanRemainingTime(userId);
            String message = remaining != null ? "您已被禁言，剩余 " + remaining : "您已被禁言，无法执行此操作";
            throw new RuntimeException(message);
        }
    }

    /**
     * 检查内容是否包含敏感词，并过滤
     */
    private String checkAndFilterContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 检查是否包含禁止词
        if (sensitiveWordFilterService.containsBlockWord(content)) {
            throw new RuntimeException("内容包含敏感词，请修改后重试");
        }

        // 过滤敏感词（将敏感词替换为**）
        return sensitiveWordFilterService.filterSensitiveWords(content);
    }

    // 发布动态（支持同步健康数据）
    @Transactional
    public Post createPost(Integer userId, String content, String images, String tag, Integer syncHealthData) {
        // 禁言检查
        checkUserBanStatus(userId);

        // 敏感词检查
        String filteredContent = checkAndFilterContent(content);

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(filteredContent);
        post.setImages(images);
        post.setTag(tag);
        post.setSyncHealthData(syncHealthData != null ? syncHealthData : 0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCreateTime(LocalDateTime.now());
        return postRepository.save(post);
    }

    // 发布动态（兼容旧版本）
    public Post createPost(Integer userId, String content, String images, String tag) {
        return createPost(userId, content, images, tag, 0);
    }

    // 获取动态列表
    public List<Map<String, Object>> getAllPosts(Integer currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Post> posts = postRepository.findAllOrderByCreateTimeDesc(pageable);
        return enrichPostsWithUserInfo(posts, currentUserId);
    }

    // 获取热门动态
    public List<Map<String, Object>> getHotPosts(Integer currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Post> posts = postRepository.findHotPosts(pageable);
        return enrichPostsWithUserInfo(posts, currentUserId);
    }

    // 获取用户动态
    public List<Map<String, Object>> getUserPosts(Integer userId, Integer currentUserId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return enrichPostsWithUserInfo(posts, currentUserId);
    }

    // 点赞
    @Transactional
    public Map<String, Object> like(Integer postId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        // 检查是否已点赞
        boolean alreadyLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        if (alreadyLiked) {
            result.put("liked", true);
            result.put("likeCount", getPostLikeCount(postId));
            result.put("message", "已经点赞过了");
            return result;
        }

        // 添加点赞
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        postLikeRepository.save(like);

        // 更新帖子点赞数
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);

            // 通知帖子作者（保存 postId 用于跳转）
            if (!post.getUserId().equals(userId)) {
                createNotification(post.getUserId(), userId, "LIKE", postId, "POST", "赞了你的帖子",
                        (long) postId, null);
            }
        }

        result.put("liked", true);
        result.put("likeCount", post != null ? post.getLikeCount() : 0);
        return result;
    }

    // 取消点赞
    @Transactional
    public Map<String, Object> unlike(Integer postId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        // 删除点赞
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);

        // 更新帖子点赞数
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
        }

        result.put("liked", false);
        result.put("likeCount", post != null ? post.getLikeCount() : 0);
        return result;
    }

    // 点赞/取消点赞（兼容旧版本）
    public Map<String, Object> toggleLike(Integer postId, Integer userId) {
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        if (liked) {
            return unlike(postId, userId);
        } else {
            return like(postId, userId);
        }
    }

    // 删除动态（用户自己删除）
    @Transactional
    public void deletePost(Integer postId, Integer userId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的帖子");
        }

        // 删除点赞记录
        postLikeRepository.deleteByPostId(postId);
        // 删除评论（包括回复）
        List<PostComment> allComments = postCommentRepository.findByPostIdOrderByCreateTimeAsc(postId);
        for (PostComment comment : allComments) {
            postCommentRepository.deleteById(comment.getId());
        }
        // 删除帖子
        postRepository.deleteById(postId);
    }

    // ========== 评论和回复功能 ==========

    // 添加顶级评论
    @Transactional
    public PostComment addComment(Integer postId, Integer userId, String content) {
        // 禁言检查
        checkUserBanStatus(userId);

        // 敏感词检查
        String filteredContent = checkAndFilterContent(content);

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(filteredContent);
        comment.setCreateTime(LocalDateTime.now());
        comment.setParentId(0);
        comment.setReplyToUserId(null);
        comment.setIsPinned(0);

        PostComment saved = postCommentRepository.save(comment);

        saved.setRootId(saved.getId());
        saved = postCommentRepository.save(saved);

        updatePostCommentCount(postId);

        // 通知帖子作者（保存 postId 用于跳转）
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            if (!post.getUserId().equals(userId)) {
                String shortContent = filteredContent.length() > 50 ? filteredContent.substring(0, 50) : filteredContent;
                createNotification(post.getUserId(), userId, "COMMENT", saved.getId(), "COMMENT",
                        "评论了你的帖子: " + shortContent, (long) postId, saved.getId().longValue());
            }
        }

        return saved;
    }

    // 回复评论
    @Transactional
    public PostComment replyComment(Integer postId, Integer userId, String content, Integer parentId, Integer replyToUserId) {
        // 禁言检查
        checkUserBanStatus(userId);

        // 敏感词检查
        String filteredContent = checkAndFilterContent(content);

        PostComment reply = new PostComment();
        reply.setPostId(postId);
        reply.setUserId(userId);
        reply.setContent(filteredContent);
        reply.setCreateTime(LocalDateTime.now());
        reply.setParentId(parentId);
        reply.setReplyToUserId(replyToUserId);
        reply.setIsPinned(0);

        PostComment saved = postCommentRepository.save(reply);

        Optional<PostComment> parentOpt = postCommentRepository.findById(parentId);
        if (parentOpt.isPresent()) {
            PostComment parent = parentOpt.get();
            if (parent.getRootId() != null && parent.getRootId() > 0) {
                saved.setRootId(parent.getRootId());
            } else {
                saved.setRootId(parentId);
            }
        } else {
            saved.setRootId(saved.getId());
        }
        saved = postCommentRepository.save(saved);

        updatePostCommentCount(postId);

        // 通知被回复的人（保存 postId 和 commentId 用于跳转）
        if (replyToUserId != null && !replyToUserId.equals(userId)) {
            String shortContent = filteredContent.length() > 50 ? filteredContent.substring(0, 50) : filteredContent;
            createNotification(replyToUserId, userId, "REPLY", saved.getId(), "COMMENT",
                    "回复了你的评论: " + shortContent, (long) postId, saved.getId().longValue());
        }

        return saved;
    }

    // 更新帖子的评论数（统计所有评论+回复）
    private void updatePostCommentCount(Integer postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            int count = postCommentRepository.findByPostIdOrderByCreateTimeAsc(postId).size();
            post.setCommentCount(count);
            postRepository.save(post);
        }
    }

    // 获取评论列表（带回复，树形结构）
    public List<Map<String, Object>> getCommentsWithReplies(Integer postId) {
        List<PostComment> topComments = postCommentRepository.findTopCommentsWithPinnedFirst(postId);
        List<PostComment> allReplies = postCommentRepository.findRepliesByPostId(postId);

        Set<Integer> userIds = new HashSet<>();
        for (PostComment c : topComments) {
            userIds.add(c.getUserId());
        }
        for (PostComment r : allReplies) {
            userIds.add(r.getUserId());
            if (r.getReplyToUserId() != null && r.getReplyToUserId() > 0) {
                userIds.add(r.getReplyToUserId());
            }
        }

        Map<Integer, User> userMap = new HashMap<>();
        for (Integer uid : userIds) {
            userRepository.findById(uid).ifPresent(user -> userMap.put(uid, user));
        }

        for (PostComment comment : topComments) {
            fillUserInfo(comment, userMap);
            if (comment.getReplyToUserId() != null && comment.getReplyToUserId() > 0) {
                User replyToUser = userMap.get(comment.getReplyToUserId());
                if (replyToUser != null) {
                    comment.setReplyToUsername(replyToUser.getNickname() != null ?
                            replyToUser.getNickname() : replyToUser.getUsername());
                }
            }
        }

        for (PostComment reply : allReplies) {
            fillUserInfo(reply, userMap);
            if (reply.getReplyToUserId() != null && reply.getReplyToUserId() > 0) {
                User replyToUser = userMap.get(reply.getReplyToUserId());
                if (replyToUser != null) {
                    reply.setReplyToUsername(replyToUser.getNickname() != null ?
                            replyToUser.getNickname() : replyToUser.getUsername());
                }
            }
        }

        Map<Integer, List<PostComment>> repliesMap = allReplies.stream()
                .collect(Collectors.groupingBy(PostComment::getParentId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (PostComment topComment : topComments) {
            Map<String, Object> commentMap = convertCommentToMap(topComment, userMap);

            int likeCount = commentLikeRepository.countByCommentId(topComment.getId());
            commentMap.put("likeCount", likeCount);
            commentMap.put("isPinned", topComment.getIsPinned() != null && topComment.getIsPinned() == 1);

            List<PostComment> childReplies = repliesMap.get(topComment.getId());
            if (childReplies != null) {
                childReplies.sort(Comparator.comparing(PostComment::getCreateTime));
                List<Map<String, Object>> replyMaps = new ArrayList<>();
                for (PostComment reply : childReplies) {
                    Map<String, Object> replyMap = convertReplyToMap(reply, userMap);
                    int replyLikeCount = commentLikeRepository.countByCommentId(reply.getId());
                    replyMap.put("likeCount", replyLikeCount);
                    replyMaps.add(replyMap);
                }
                commentMap.put("replies", replyMaps);
            } else {
                commentMap.put("replies", new ArrayList<>());
            }

            result.add(commentMap);
        }

        return result;
    }

    // 填充用户信息
    private void fillUserInfo(PostComment comment, Map<Integer, User> userMap) {
        User user = userMap.get(comment.getUserId());
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            comment.setUserAvatar(user.getAvatar());
        }
    }

    // 将评论转换为前端格式
    private Map<String, Object> convertCommentToMap(PostComment comment, Map<Integer, User> userMap) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("postId", comment.getPostId());
        map.put("userId", comment.getUserId());
        map.put("content", comment.getContent());
        map.put("createTime", comment.getCreateTime());
        map.put("parentId", comment.getParentId());
        map.put("replyToUserId", comment.getReplyToUserId());
        map.put("replyToUsername", comment.getReplyToUsername());

        User user = userMap.get(comment.getUserId());
        if (user != null) {
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            map.put("userAvatar", user.getAvatar());
        } else {
            map.put("username", "未知用户");
            map.put("nickname", "未知用户");
            map.put("userAvatar", null);
        }

        return map;
    }

    // 将回复转换为前端格式
    private Map<String, Object> convertReplyToMap(PostComment reply, Map<Integer, User> userMap) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reply.getId());
        map.put("postId", reply.getPostId());
        map.put("userId", reply.getUserId());
        map.put("content", reply.getContent());
        map.put("createTime", reply.getCreateTime());
        map.put("parentId", reply.getParentId());
        map.put("replyToUserId", reply.getReplyToUserId());
        map.put("replyToUsername", reply.getReplyToUsername());

        User user = userMap.get(reply.getUserId());
        if (user != null) {
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            map.put("userAvatar", user.getAvatar());
        } else {
            map.put("username", "未知用户");
            map.put("nickname", "未知用户");
            map.put("userAvatar", null);
        }

        return map;
    }

    // 获取评论列表（简单版，兼容旧接口）
    public List<Map<String, Object>> getComments(Integer postId) {
        List<PostComment> comments = postCommentRepository.findByPostIdOrderByCreateTimeAsc(postId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PostComment comment : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", comment.getId());
            item.put("content", comment.getContent());
            item.put("createTime", comment.getCreateTime());
            item.put("userId", comment.getUserId());
            item.put("parentId", comment.getParentId());
            item.put("replyToUserId", comment.getReplyToUserId());

            User user = userRepository.findById(comment.getUserId()).orElse(null);
            if (user != null) {
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                item.put("userAvatar", user.getAvatar() != null ? user.getAvatar() : "");
            }

            if (comment.getReplyToUserId() != null && comment.getReplyToUserId() > 0) {
                User replyToUser = userRepository.findById(comment.getReplyToUserId()).orElse(null);
                if (replyToUser != null) {
                    item.put("replyToUsername", replyToUser.getNickname() != null ?
                            replyToUser.getNickname() : replyToUser.getUsername());
                }
            }

            result.add(item);
        }

        return result;
    }

    // 删除评论（用户自己删除）
    @Transactional
    public void deleteComment(Integer commentId, Integer userId) {
        PostComment comment = postCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        Post post = postOpt.orElse(null);

        if (!comment.getUserId().equals(userId) && (post == null || !post.getUserId().equals(userId))) {
            throw new RuntimeException("无权删除此评论");
        }

        if (comment.getParentId() == null || comment.getParentId() == 0) {
            List<PostComment> replies = postCommentRepository.findByParentIdOrderByCreateTimeAsc(commentId);
            for (PostComment reply : replies) {
                commentLikeRepository.deleteByCommentIdAndUserId(reply.getId(), userId);
                postCommentRepository.deleteById(reply.getId());
            }
        }

        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
        postCommentRepository.deleteById(commentId);

        if (post != null) {
            updatePostCommentCount(post.getId());
        }
    }

    // 删除回复（用户自己删除）
    @Transactional
    public void deleteReply(Integer replyId, Integer userId) {
        PostComment reply = postCommentRepository.findById(replyId).orElse(null);
        if (reply == null) {
            throw new RuntimeException("回复不存在");
        }

        Optional<Post> postOpt = postRepository.findById(reply.getPostId());
        Post post = postOpt.orElse(null);

        if (!reply.getUserId().equals(userId) && (post == null || !post.getUserId().equals(userId))) {
            throw new RuntimeException("无权删除此回复");
        }

        commentLikeRepository.deleteByCommentIdAndUserId(replyId, userId);
        postCommentRepository.deleteById(replyId);

        if (post != null) {
            updatePostCommentCount(post.getId());
        }
    }

    // ========== 评论点赞功能 ==========

    @Transactional
    public Map<String, Object> likeComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        boolean alreadyLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        if (alreadyLiked) {
            result.put("liked", true);
            result.put("likeCount", commentLikeRepository.countByCommentId(commentId));
            result.put("message", "已经点赞过了");
            return result;
        }

        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        commentLikeRepository.save(like);

        int likeCount = commentLikeRepository.countByCommentId(commentId);
        result.put("liked", true);
        result.put("likeCount", likeCount);

        // 通知评论作者（需要获取 postId 用于跳转）
        Optional<PostComment> commentOpt = postCommentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            PostComment comment = commentOpt.get();
            if (!comment.getUserId().equals(userId)) {
                // 获取帖子ID
                Integer postId = comment.getPostId();
                createNotification(comment.getUserId(), userId, "LIKE", commentId, "COMMENT",
                        "赞了你的评论", (long) postId, (long) commentId);
            }
        }

        return result;
    }

    @Transactional
    public Map<String, Object> unlikeComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

        result.put("liked", false);
        result.put("likeCount", commentLikeRepository.countByCommentId(commentId));

        return result;
    }

    // ========== 置顶评论功能 ==========

    @Transactional
    public Map<String, Object> pinComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        PostComment comment = postCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        Post post = postOpt.orElse(null);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new RuntimeException("只有帖子作者可以置顶评论");
        }

        if (comment.getIsPinned() != null && comment.getIsPinned() == 1) {
            comment.setIsPinned(0);
            result.put("pinned", false);
            result.put("message", "已取消置顶");
        } else {
            comment.setIsPinned(1);
            result.put("pinned", true);
            result.put("message", "已置顶");
        }

        postCommentRepository.save(comment);
        return result;
    }

    // ========== 通知功能 ==========

    /**
     * 创建通知（新版本，支持保存 postId 和 commentId）
     */
    @Transactional
    public void createNotification(Integer userId, Integer fromUserId, String type,
                                   Integer targetId, String targetType, String content,
                                   Long postId, Long commentId) {
        if (userId.equals(fromUserId)) return;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setFromUserId(fromUserId);
        notification.setType(type);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setContent(content);
        notification.setCreateTime(LocalDateTime.now());
        notification.setIsRead(0);

        // 保存关联的帖子ID和评论ID（用于前端跳转）
        notification.setPostId(postId);
        notification.setCommentId(commentId);

        notificationRepository.save(notification);
    }

    /**
     * 创建通知（旧版本，兼容原有调用，不保存 postId）
     */
    @Transactional
    public void createNotification(Integer userId, Integer fromUserId, String type,
                                   Integer targetId, String targetType, String content) {
        createNotification(userId, fromUserId, type, targetId, targetType, content, null, null);
    }

    public Map<String, Object> getNotifications(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.domain.Page<Notification> notificationPage =
                notificationRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);

        for (Notification notification : notificationPage.getContent()) {
            userRepository.findById(notification.getFromUserId()).ifPresent(user -> {
                notification.setFromUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
                notification.setFromUserAvatar(user.getAvatar());
            });
        }

        int unreadCount = notificationRepository.countByUserIdAndIsRead(userId, 0);

        Map<String, Object> result = new HashMap<>();
        result.put("notifications", notificationPage.getContent());
        result.put("unreadCount", unreadCount);
        result.put("totalPages", notificationPage.getTotalPages());
        result.put("totalElements", notificationPage.getTotalElements());

        return result;
    }

    @Transactional
    public void markNotificationAsRead(Integer notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllNotificationsAsRead(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsRead(userId, 0);
        for (Notification notification : notifications) {
            notification.setIsRead(1);
            notificationRepository.save(notification);
        }
    }

    public int getUnreadNotificationCount(Integer userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, 0);
    }

    // ========== 获取用户禁言状态（供前端调用） ==========

    public Map<String, Object> getUserBanStatus(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        boolean isBanned = user.getIsBanned() != null && user.getIsBanned() == 1;

        // 检查是否过期
        if (isBanned && user.getBanEndTime() != null && user.getBanEndTime().isBefore(LocalDateTime.now())) {
            isBanned = false;
            user.setIsBanned(0);
            user.setBanEndTime(null);
            userRepository.save(user);
        }

        result.put("success", true);
        result.put("isBanned", isBanned);
        result.put("banEndTime", user.getBanEndTime());
        result.put("remainingTime", getBanRemainingTime(userId));
        return result;
    }

    // ========== 判断用户是否点赞了评论 ==========
    public boolean isCommentLiked(Integer commentId, Integer userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    // ========== 管理员社区管理方法 ==========

    /**
     * 获取所有帖子（管理员用，分页）
     */
    public Page<Post> getAllPostsForAdmin(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return postRepository.searchByKeyword(keyword, pageable);
        }
        return postRepository.findAllForAdmin(pageable);
    }

    /**
     * 管理员删除帖子
     */
    @Transactional
    public void adminDeletePost(Integer postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        // 删除点赞记录
        postLikeRepository.deleteByPostId(postId);
        // 删除评论（包括回复）
        List<PostComment> allComments = postCommentRepository.findByPostIdOrderByCreateTimeAsc(postId);
        for (PostComment comment : allComments) {
            postCommentRepository.deleteById(comment.getId());
        }
        // 删除帖子
        postRepository.deleteById(postId);
    }

    /**
     * 置顶帖子
     */
    @Transactional
    public void pinPost(Integer postId, Integer isPinned) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        postRepository.updatePinStatus(postId, isPinned);
    }

    /**
     * 加精帖子
     */
    @Transactional
    public void featurePost(Integer postId, Integer isFeatured) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        postRepository.updateFeaturedStatus(postId, isFeatured);
    }

    /**
     * 获取所有评论（管理员用，分页）
     */
    public Page<PostComment> getAllCommentsForAdmin(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return postCommentRepository.searchByKeyword(keyword, pageable);
        }
        return postCommentRepository.findAllForAdmin(pageable);
    }

    /**
     * 管理员删除评论
     */
    @Transactional
    public void adminDeleteComment(Integer commentId) {
        PostComment comment = postCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 获取帖子ID以便更新评论数
        Integer postId = comment.getPostId();

        // 如果是顶级评论，删除其下的所有回复
        if (comment.getParentId() == null || comment.getParentId() == 0) {
            List<PostComment> replies = postCommentRepository.findByParentIdOrderByCreateTimeAsc(commentId);
            for (PostComment reply : replies) {
                commentLikeRepository.deleteByCommentIdAndUserId(reply.getId(), null);
                postCommentRepository.deleteById(reply.getId());
            }
        }

        // 删除评论的点赞记录
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, null);
        // 删除评论
        postCommentRepository.deleteById(commentId);

        // 更新帖子评论数
        updatePostCommentCount(postId);
    }

    /**
     * 批量删除评论
     */
    @Transactional
    public void batchDeleteComments(List<Integer> commentIds) {
        for (Integer commentId : commentIds) {
            adminDeleteComment(commentId);
        }
    }

    // 上传图片
    public String uploadImage(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/community/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + suffix;
            String filePath = uploadDir + fileName;
            file.transferTo(new File(filePath));

            return "/uploads/community/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    // 获取帖子点赞数
    private int getPostLikeCount(Integer postId) {
        Post post = postRepository.findById(postId).orElse(null);
        return post != null ? post.getLikeCount() : 0;
    }

    // 补充动态信息
    private List<Map<String, Object>> enrichPostsWithUserInfo(List<Post> posts, Integer currentUserId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Post post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", post.getId());
            item.put("content", post.getContent());
            item.put("userId", post.getUserId());
            item.put("isPinned", post.getIsPinned() != null ? post.getIsPinned() : 0);
            item.put("isFeatured", post.getIsFeatured() != null ? post.getIsFeatured() : 0);

            if (post.getImages() != null && !post.getImages().isEmpty()) {
                item.put("images", Arrays.asList(post.getImages().split(",")));
            } else {
                item.put("images", new ArrayList<>());
            }

            item.put("tag", post.getTag());
            item.put("likeCount", post.getLikeCount());
            item.put("commentCount", post.getCommentCount());
            item.put("createTime", post.getCreateTime());

            User user = userRepository.findById(post.getUserId()).orElse(null);
            if (user != null) {
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());

                String avatar = user.getAvatar();
                if (avatar != null && !avatar.isEmpty()) {
                    item.put("userAvatar", avatar);
                } else {
                    item.put("userAvatar", null);
                }
            } else {
                item.put("username", "未知用户");
                item.put("nickname", "未知用户");
                item.put("userAvatar", null);
            }

            if (currentUserId != null) {
                item.put("liked", postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
            } else {
                item.put("liked", false);
            }

            result.add(item);
        }
        return result;
    }

    private String getAvatarEmoji(String gender) {
        if ("1".equals(gender)) return "👨";
        if ("0".equals(gender)) return "👩";
        return "👤";
    }
}