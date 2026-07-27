package com.health.health_system.controller;

import com.health.health_system.repository.BanRecordRepository;
import com.health.health_system.repository.HealthRecordRepository;
import com.health.health_system.entity.BanRecord;
import com.health.health_system.entity.HealthArticle;
import com.health.health_system.entity.User;
import com.health.health_system.entity.ItemLibrary;
import com.health.health_system.entity.Post;
import com.health.health_system.entity.PostComment;
import com.health.health_system.entity.Announcement;
import com.health.health_system.entity.Report;
import com.health.health_system.repository.HealthArticleRepository;
import com.health.health_system.repository.UserRepository;
import com.health.health_system.repository.ItemLibraryRepository;
import com.health.health_system.repository.PostRepository;
import com.health.health_system.repository.PostCommentRepository;
import com.health.health_system.repository.AnnouncementRepository;
import com.health.health_system.repository.ReportRepository;
import com.health.health_system.service.CommunityService;
import com.health.health_system.service.AnnouncementService;
import com.health.health_system.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemLibraryRepository itemLibraryRepository;

    @Autowired
    private HealthArticleRepository healthArticleRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BanRecordRepository banRecordRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private ReportRepository reportRepository;

    // ========== 用户管理 ==========

    // 获取所有用户（分页+搜索）
    @GetMapping("/users")
    public Map<String, Object> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userRepository.findByUsernameContaining(keyword, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        result.put("success", true);
        result.put("data", userPage.getContent());
        result.put("total", userPage.getTotalElements());
        result.put("totalPages", userPage.getTotalPages());
        return result;
    }

    // 更新用户信息
    @PutMapping("/user/{userId}")
    public Map<String, Object> updateUser(@PathVariable Integer userId, @RequestBody Map<String, Object> updates) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        User user = userOpt.get();
        if (updates.containsKey("nickname")) {
            user.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("role")) {
            user.setRole((String) updates.get("role"));
        }
        userRepository.save(user);
        result.put("success", true);
        return result;
    }

    // 重置密码
    @PostMapping("/user/{userId}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        User user = userOpt.get();
        user.setPassword("123456");
        userRepository.save(user);
        result.put("success", true);
        result.put("message", "密码已重置为123456");
        return result;
    }

    // 切换用户状态（启用/禁用）
    @PutMapping("/user/{userId}/status")
    public Map<String, Object> toggleUserStatus(@PathVariable Integer userId, @RequestParam Integer status) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        User user = userOpt.get();
        user.setStatus(status);
        userRepository.save(user);
        result.put("success", true);
        return result;
    }

    // 根据ID删除用户
    @DeleteMapping("/user/{userId}")
    public Map<String, Object> deleteUser(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 不允许删除管理员自己（防止误删）
        if (user.get().getRole().equals("ADMIN")) {
            result.put("success", false);
            result.put("message", "不能删除管理员账号");
            return result;
        }

        userRepository.deleteById(userId);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // ========== 禁言功能 ==========

    // 禁言用户
    @PostMapping("/user/{userId}/ban")
    public Map<String, Object> banUser(
            @PathVariable Integer userId,
            @RequestParam Integer days,
            @RequestParam(required = false) String reason,
            @RequestParam Integer adminId) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime banEndTime;

        if (days == 36500) {
            banEndTime = now.plusYears(100);
        } else {
            banEndTime = now.plusDays(days);
        }

        user.setIsBanned(1);
        user.setBanEndTime(banEndTime);
        userRepository.save(user);

        // 记录禁言日志
        BanRecord banRecord = new BanRecord();
        banRecord.setUserId(userId);
        banRecord.setAdminId(adminId);
        banRecord.setBanDays(days);
        banRecord.setReason(reason);
        banRecord.setBanStartTime(now);
        banRecord.setBanEndTime(banEndTime);
        banRecord.setIsActive(1);
        banRecord.setCreateTime(now);
        banRecordRepository.save(banRecord);

        result.put("success", true);
        result.put("message", days == 36500 ? "用户已被永久禁言" : "用户已被禁言 " + days + " 天");
        return result;
    }

    // 解除禁言
    @PostMapping("/user/{userId}/unban")
    public Map<String, Object> unbanUser(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        user.setIsBanned(0);
        user.setBanEndTime(null);
        userRepository.save(user);

        // 更新禁言记录状态
        List<BanRecord> records = banRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (!records.isEmpty()) {
            BanRecord latest = records.get(0);
            latest.setIsActive(0);
            banRecordRepository.save(latest);
        }

        result.put("success", true);
        result.put("message", "用户已解除禁言");
        return result;
    }

    // 检查用户禁言状态
    @GetMapping("/user/{userId}/ban-status")
    public Map<String, Object> getBanStatus(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        boolean isBanned = user.getIsBanned() == 1;
        LocalDateTime banEndTime = user.getBanEndTime();

        // 检查是否过期
        if (isBanned && banEndTime != null && banEndTime.isBefore(LocalDateTime.now())) {
            isBanned = false;
            user.setIsBanned(0);
            user.setBanEndTime(null);
            userRepository.save(user);
        }

        result.put("success", true);
        result.put("isBanned", isBanned);
        result.put("banEndTime", banEndTime);
        return result;
    }

    // ========== 运动/饮食库管理 ==========

    // 获取所有库项目
    @GetMapping("/library")
    public List<ItemLibrary> getAllLibrary() {
        return itemLibraryRepository.findAll();
    }

    // 按类型获取（FOOD 或 SPORT）
    @GetMapping("/library/type/{type}")
    public List<ItemLibrary> getLibraryByType(@PathVariable String type) {
        return itemLibraryRepository.findByType(type);
    }

    // 添加库项目
    @PostMapping("/library/add")
    public Map<String, Object> addLibraryItem(
            @RequestParam String type,
            @RequestParam String name,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Integer calories,
            @RequestParam(required = false) String remark) {

        Map<String, Object> result = new HashMap<>();

        ItemLibrary item = new ItemLibrary();
        item.setType(type);
        item.setName(name);
        item.setUnit(unit);
        item.setCalories(calories != null ? calories : 0);
        item.setRemark(remark);

        itemLibraryRepository.save(item);

        result.put("success", true);
        result.put("message", "添加成功");
        result.put("item", item);
        return result;
    }

    // 删除库项目
    @DeleteMapping("/library/{itemId}")
    public Map<String, Object> deleteLibraryItem(@PathVariable Integer itemId) {
        Map<String, Object> result = new HashMap<>();

        if (!itemLibraryRepository.existsById(itemId)) {
            result.put("success", false);
            result.put("message", "项目不存在");
            return result;
        }

        itemLibraryRepository.deleteById(itemId);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // 更新库项目
    @PutMapping("/library/{itemId}")
    public Map<String, Object> updateLibraryItem(
            @PathVariable Integer itemId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Integer calories,
            @RequestParam(required = false) String remark) {

        Map<String, Object> result = new HashMap<>();

        Optional<ItemLibrary> optional = itemLibraryRepository.findById(itemId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "项目不存在");
            return result;
        }

        ItemLibrary item = optional.get();
        if (name != null) item.setName(name);
        if (unit != null) item.setUnit(unit);
        if (calories != null) item.setCalories(calories);
        if (remark != null) item.setRemark(remark);

        itemLibraryRepository.save(item);

        result.put("success", true);
        result.put("message", "更新成功");
        result.put("item", item);
        return result;
    }

    // ========== 文章管理 ==========

    // 获取所有文章（支持分页和搜索）
    @GetMapping("/articles")
    public Map<String, Object> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size);
        Page<HealthArticle> articlePage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            articlePage = healthArticleRepository.searchByKeyword(keyword, pageable);
        } else {
            articlePage = healthArticleRepository.findAll(pageable);
        }

        result.put("success", true);
        result.put("data", articlePage.getContent());
        result.put("total", articlePage.getTotalElements());
        result.put("totalPages", articlePage.getTotalPages());
        result.put("currentPage", page);

        return result;
    }

    // 创建文章
    @PostMapping("/article")
    public Map<String, Object> createArticle(@RequestBody HealthArticle article) {
        Map<String, Object> result = new HashMap<>();
        try {
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCreateTime(LocalDateTime.now());
            article.setUpdateTime(LocalDateTime.now());
            article.setStatus(article.getStatus() != null ? article.getStatus() : 1);

            HealthArticle saved = healthArticleRepository.save(article);
            result.put("success", true);
            result.put("data", saved);
            result.put("message", "文章创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 更新文章
    @PutMapping("/article/{id}")
    public Map<String, Object> updateArticle(@PathVariable Integer id, @RequestBody HealthArticle article) {
        Map<String, Object> result = new HashMap<>();

        Optional<HealthArticle> existingOpt = healthArticleRepository.findById(id);
        if (existingOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        HealthArticle existing = existingOpt.get();
        if (article.getTitle() != null) existing.setTitle(article.getTitle());
        if (article.getSummary() != null) existing.setSummary(article.getSummary());
        if (article.getContent() != null) existing.setContent(article.getContent());
        if (article.getCategory() != null) existing.setCategory(article.getCategory());
        if (article.getStatus() != null) existing.setStatus(article.getStatus());
        existing.setUpdateTime(LocalDateTime.now());

        healthArticleRepository.save(existing);
        result.put("success", true);
        result.put("message", "文章更新成功");
        return result;
    }

    // 删除文章
    @DeleteMapping("/article/{id}")
    public Map<String, Object> deleteArticle(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        if (!healthArticleRepository.existsById(id)) {
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        healthArticleRepository.deleteById(id);
        result.put("success", true);
        result.put("message", "文章删除成功");
        return result;
    }

    // ========== 数据统计 ==========

    // 获取统计数据
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalArticles = healthArticleRepository.count();

        Integer totalViews = 0;
        Integer totalLikes = 0;

        List<HealthArticle> allArticles = healthArticleRepository.findAll();
        for (HealthArticle article : allArticles) {
            totalViews += article.getViewCount() != null ? article.getViewCount() : 0;
            totalLikes += article.getLikeCount() != null ? article.getLikeCount() : 0;
        }

        result.put("totalUsers", totalUsers);
        result.put("totalArticles", totalArticles);
        result.put("totalViews", totalViews);
        result.put("totalLikes", totalLikes);

        return result;
    }

    // ========== 统计图表接口 ==========

    @GetMapping("/statistics/user-trend")
    public Map<String, Object> getUserTrend(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days - 1);
        List<User> users = userRepository.findAll();
        Map<String, Integer> dailyCount = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 0; i < days; i++) {
            String date = startDate.plusDays(i).format(formatter);
            dailyCount.put(date, 0);
        }

        for (User user : users) {
            if (user.getCreateTime() != null && user.getCreateTime().isAfter(startDate)) {
                String date = user.getCreateTime().format(formatter);
                dailyCount.put(date, dailyCount.getOrDefault(date, 0) + 1);
            }
        }

        List<String> dates = new ArrayList<>(dailyCount.keySet());
        List<Integer> counts = new ArrayList<>(dailyCount.values());

        result.put("success", true);
        result.put("dates", dates);
        result.put("counts", counts);
        result.put("total", counts.stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    @GetMapping("/statistics/post-trend")
    public Map<String, Object> getPostTrend(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days - 1);
        List<Post> posts = postRepository.findAll();
        Map<String, Integer> dailyCount = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 0; i < days; i++) {
            String date = startDate.plusDays(i).format(formatter);
            dailyCount.put(date, 0);
        }

        for (Post post : posts) {
            if (post.getCreateTime() != null && post.getCreateTime().isAfter(startDate)) {
                String date = post.getCreateTime().format(formatter);
                dailyCount.put(date, dailyCount.getOrDefault(date, 0) + 1);
            }
        }

        List<String> dates = new ArrayList<>(dailyCount.keySet());
        List<Integer> counts = new ArrayList<>(dailyCount.values());

        result.put("success", true);
        result.put("dates", dates);
        result.put("counts", counts);
        result.put("total", counts.stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    @GetMapping("/statistics/user-activity")
    public Map<String, Object> getUserActivity() {
        Map<String, Object> result = new HashMap<>();

        List<User> users = userRepository.findAll();
        Map<Integer, Integer> postCountByUser = new HashMap<>();
        List<Post> allPosts = postRepository.findAll();

        for (Post post : allPosts) {
            postCountByUser.put(post.getUserId(), postCountByUser.getOrDefault(post.getUserId(), 0) + 1);
        }

        int activeUsers = 0, normalUsers = 0, inactiveUsers = 0;
        for (User user : users) {
            if ("ADMIN".equals(user.getRole())) continue;
            int postCount = postCountByUser.getOrDefault(user.getId(), 0);
            if (postCount >= 5) activeUsers++;
            else if (postCount >= 1) normalUsers++;
            else inactiveUsers++;
        }

        result.put("success", true);
        result.put("activeUsers", activeUsers);
        result.put("normalUsers", normalUsers);
        result.put("inactiveUsers", inactiveUsers);
        result.put("total", activeUsers + normalUsers + inactiveUsers);
        return result;
    }

    @GetMapping("/statistics/dashboard")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalArticles = healthArticleRepository.count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<User> allUsers = userRepository.findAll();
        long todayNewUsers = allUsers.stream()
                .filter(u -> u.getCreateTime() != null && u.getCreateTime().isAfter(todayStart))
                .count();

        List<Post> allPosts = postRepository.findAll();
        long todayNewPosts = allPosts.stream()
                .filter(p -> p.getCreateTime() != null && p.getCreateTime().isAfter(todayStart))
                .count();

        result.put("success", true);
        result.put("totalUsers", totalUsers);
        result.put("totalPosts", totalPosts);
        result.put("totalArticles", totalArticles);
        result.put("todayNewUsers", todayNewUsers);
        result.put("todayNewPosts", todayNewPosts);
        return result;
    }

    // ========== 社区管理 - 帖子管理接口 ==========

    /**
     * 获取所有帖子（管理员）- 支持排序，带用户信息
     */
    @GetMapping("/posts")
    public Map<String, Object> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        Map<String, Object> result = new HashMap<>();

        // 获取所有帖子
        List<Post> posts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            posts = postRepository.findByContentContaining(keyword);
        } else {
            posts = postRepository.findAll();
        }

        // 根据 sort 参数排序
        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "time_desc":
                    posts.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
                    break;
                case "time_asc":
                    posts.sort((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()));
                    break;
                case "likes_desc":
                    posts.sort((a, b) -> (b.getLikeCount() != null ? b.getLikeCount() : 0) - (a.getLikeCount() != null ? a.getLikeCount() : 0));
                    break;
                case "likes_asc":
                    posts.sort((a, b) -> (a.getLikeCount() != null ? a.getLikeCount() : 0) - (b.getLikeCount() != null ? b.getLikeCount() : 0));
                    break;
                case "comments_desc":
                    posts.sort((a, b) -> (b.getCommentCount() != null ? b.getCommentCount() : 0) - (a.getCommentCount() != null ? a.getCommentCount() : 0));
                    break;
                case "comments_asc":
                    posts.sort((a, b) -> (a.getCommentCount() != null ? a.getCommentCount() : 0) - (b.getCommentCount() != null ? b.getCommentCount() : 0));
                    break;
                default:
                    posts.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
            }
        } else {
            posts.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        }

        // 为每个帖子补充用户信息（username 和 nickname）
        List<Map<String, Object>> postsWithUserInfo = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", post.getId());
            postMap.put("userId", post.getUserId());
            postMap.put("content", post.getContent());
            postMap.put("images", post.getImages());
            postMap.put("likeCount", post.getLikeCount());
            postMap.put("commentCount", post.getCommentCount());
            postMap.put("isPinned", post.getIsPinned());
            postMap.put("isFeatured", post.getIsFeatured());
            postMap.put("createTime", post.getCreateTime());

            Optional<User> userOpt = userRepository.findById(post.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                postMap.put("username", user.getUsername());
                postMap.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            } else {
                postMap.put("username", "未知用户");
                postMap.put("nickname", "未知用户");
            }

            postsWithUserInfo.add(postMap);
        }

        // 分页
        int start = page * size;
        int end = Math.min(start + size, postsWithUserInfo.size());
        List<Map<String, Object>> pageContent = start < postsWithUserInfo.size() ? postsWithUserInfo.subList(start, end) : new ArrayList<>();

        result.put("success", true);
        result.put("data", pageContent);
        result.put("total", postsWithUserInfo.size());
        result.put("totalPages", (int) Math.ceil((double) postsWithUserInfo.size() / size));
        return result;
    }

    /**
     * 管理员删除帖子
     */
    @DeleteMapping("/post/{postId}")
    public Map<String, Object> adminDeletePost(@PathVariable Integer postId) {
        Map<String, Object> result = new HashMap<>();
        try {
            communityService.adminDeletePost(postId);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 置顶/取消置顶帖子
     */
    @PutMapping("/post/{postId}/pin")
    public Map<String, Object> togglePinPost(@PathVariable Integer postId, @RequestParam Integer isPinned) {
        Map<String, Object> result = new HashMap<>();
        try {
            communityService.pinPost(postId, isPinned);
            result.put("success", true);
            result.put("message", isPinned == 1 ? "置顶成功" : "取消置顶成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 加精/取消加精帖子
     */
    @PutMapping("/post/{postId}/feature")
    public Map<String, Object> toggleFeaturePost(@PathVariable Integer postId, @RequestParam Integer isFeatured) {
        Map<String, Object> result = new HashMap<>();
        try {
            communityService.featurePost(postId, isFeatured);
            result.put("success", true);
            result.put("message", isFeatured == 1 ? "加精成功" : "取消加精成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ========== 社区管理 - 评论管理接口 ==========

    /**
     * 获取所有评论（管理员）
     */
    @GetMapping("/comments")
    public Map<String, Object> getAllComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        Page<PostComment> commentPage = communityService.getAllCommentsForAdmin(page, size, keyword);

        // 补充评论关联的用户信息
        for (PostComment comment : commentPage.getContent()) {
            userRepository.findById(comment.getUserId()).ifPresent(user -> {
                comment.setUsername(user.getUsername());
                comment.setNickname(user.getNickname());
                comment.setUserAvatar(user.getAvatar());
            });
        }

        result.put("success", true);
        result.put("data", commentPage.getContent());
        result.put("total", commentPage.getTotalElements());
        result.put("totalPages", commentPage.getTotalPages());
        return result;
    }

    /**
     * 管理员删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    public Map<String, Object> adminDeleteComment(@PathVariable Integer commentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            communityService.adminDeleteComment(commentId);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 批量删除评论
     */
    @PostMapping("/comments/batch-delete")
    public Map<String, Object> batchDeleteComments(@RequestBody List<Integer> commentIds) {
        Map<String, Object> result = new HashMap<>();
        try {
            communityService.batchDeleteComments(commentIds);
            result.put("success", true);
            result.put("message", "批量删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ========== 社区管理 - 举报处理接口 ==========

    /**
     * 用户举报帖子或评论
     */
    @PostMapping("/report")
    public Map<String, Object> addReport(
            @RequestParam Integer reporterId,
            @RequestParam String targetType,
            @RequestParam Integer targetId,
            @RequestParam Integer targetUserId,
            @RequestParam String reason) {

        Map<String, Object> result = new HashMap<>();

        try {
            Report report = new Report();
            report.setReporterId(reporterId);
            report.setTargetType(targetType);
            report.setTargetId(Long.valueOf(targetId));
            report.setTargetUserId(targetUserId);
            report.setReason(reason);
            report.setStatus(0);
            report.setCreateTime(LocalDateTime.now());

            reportRepository.save(report);

            result.put("success", true);
            result.put("message", "举报已提交，我们会尽快处理");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取举报列表（管理员）
     */
    @GetMapping("/reports-list")
    public Map<String, Object> getReportsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reportPage;

        if (status != null) {
            reportPage = reportRepository.findByStatus(status, pageable);
        } else {
            reportPage = reportRepository.findAll(pageable);
        }

        // 补充举报人信息
        for (Report report : reportPage.getContent()) {
            userRepository.findById(report.getReporterId()).ifPresent(user -> {
                report.setReporterName(user.getUsername());
            });

            // 获取被举报的内容预览
            if ("POST".equals(report.getTargetType())) {
                postRepository.findById(report.getTargetId().intValue()).ifPresent(post -> {
                    String content = post.getContent();
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }
                    report.setTargetContent(content);
                });
            } else if ("COMMENT".equals(report.getTargetType())) {
                postCommentRepository.findById(report.getTargetId().intValue()).ifPresent(comment -> {
                    String content = comment.getContent();
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }
                    report.setTargetContent(content);
                });
            }
        }

        result.put("success", true);
        result.put("data", reportPage.getContent());
        result.put("total", reportPage.getTotalElements());
        result.put("totalPages", reportPage.getTotalPages());
        return result;
    }

    /**
     * 获取举报列表（原接口，保持兼容）
     */
    @GetMapping("/reports")
    public Map<String, Object> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return getReportsList(page, size, status);
    }

    /**
     * 处理举报
     * @param reportId 举报ID
     * @param action 1:通过(删除内容), 2:驳回
     * @param handleNote 处理备注
     * @param adminId 管理员ID
     */
    @PostMapping("/report/{reportId}/handle")
    public Map<String, Object> handleReport(
            @PathVariable Long reportId,
            @RequestParam Integer action,
            @RequestParam(required = false) String handleNote,
            @RequestParam Integer adminId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Report report = reportRepository.findById(reportId).orElse(null);
            if (report == null) {
                result.put("success", false);
                result.put("message", "举报不存在");
                return result;
            }

            // 如果通过（action=1），删除被举报的内容
            if (action == 1) {
                if ("POST".equals(report.getTargetType())) {
                    communityService.adminDeletePost(report.getTargetId().intValue());
                } else if ("COMMENT".equals(report.getTargetType())) {
                    communityService.adminDeleteComment(report.getTargetId().intValue());
                }
            }

            reportService.handleReport(reportId, adminId, action, handleNote);
            result.put("success", true);
            result.put("message", action == 1 ? "已通过举报，内容已删除" : "已驳回举报");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ========== 社区管理 - 公告管理接口 ==========

    /**
     * 获取所有公告（管理员）
     */
    @GetMapping("/announcements")
    public Map<String, Object> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = new HashMap<>();
        Page<Announcement> announcementPage = announcementService.getAllAnnouncements(page, size);

        result.put("success", true);
        result.put("data", announcementPage.getContent());
        result.put("total", announcementPage.getTotalElements());
        result.put("totalPages", announcementPage.getTotalPages());
        return result;
    }

    /**
     * 创建公告
     */
    @PostMapping("/announcement")
    public Map<String, Object> createAnnouncement(@RequestBody Announcement announcement) {
        Map<String, Object> result = new HashMap<>();
        try {
            Announcement saved = announcementService.createAnnouncement(announcement);
            result.put("success", true);
            result.put("data", saved);
            result.put("message", "创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新公告
     */
    @PutMapping("/announcement/{id}")
    public Map<String, Object> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        Map<String, Object> result = new HashMap<>();
        try {
            Announcement updated = announcementService.updateAnnouncement(id, announcement);
            result.put("success", true);
            result.put("data", updated);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/announcement/{id}")
    public Map<String, Object> deleteAnnouncement(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            announcementService.deleteAnnouncement(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 启用/禁用公告
     */
    @PutMapping("/announcement/{id}/toggle")
    public Map<String, Object> toggleAnnouncement(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            announcementService.toggleAnnouncement(id);
            result.put("success", true);
            result.put("message", "状态已更新");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ========== 社区管理 - 批量删除帖子接口 ==========

    /**
     * 批量删除帖子
     */
    @PostMapping("/posts/batch-delete")
    public Map<String, Object> batchDeletePosts(@RequestBody List<Integer> postIds) {
        Map<String, Object> result = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要删除的帖子");
            return result;
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (Integer postId : postIds) {
            try {
                communityService.adminDeletePost(postId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errorMsg.append("帖子ID ").append(postId).append(": ").append(e.getMessage()).append("; ");
            }
        }

        result.put("success", true);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        if (failCount > 0) {
            result.put("message", "成功删除 " + successCount + " 条，失败 " + failCount + " 条：" + errorMsg.toString());
        } else {
            result.put("message", "成功删除 " + successCount + " 条帖子");
        }
        return result;
    }
}