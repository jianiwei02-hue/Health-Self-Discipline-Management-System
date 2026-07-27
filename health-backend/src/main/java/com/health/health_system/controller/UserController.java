package com.health.health_system.controller;

import com.health.health_system.entity.User;
import com.health.health_system.repository.PostRepository;
import com.health.health_system.repository.UserRepository;
import com.health.health_system.service.CommunityService;
import com.health.health_system.service.FollowService;
import com.health.health_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FollowService followService;

    @Autowired
    private CommunityService communityService;

    // 注册接口
    @PostMapping("/register")
    public Map<String, Object> register(@RequestParam String username,
                                        @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        String msg = userService.register(username, password);
        result.put("success", msg.equals("注册成功"));
        result.put("message", msg);
        if (msg.equals("注册成功")) {
            User user = userService.login(username, password);
            if (user != null) {
                result.put("userId", user.getId());
            }
        }
        return result;
    }

    // 登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username,
                                     @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.login(username, password);
        if (user != null) {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("role", user.getRole());
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    // 获取用户信息
    @GetMapping("/{userId}")
    public User getUserById(@PathVariable Integer userId) {
        return userService.getUserById(userId);
    }

    // 更新用户信息
    @PostMapping("/update")
    public Map<String, Object> updateUser(@RequestParam Integer userId,
                                          @RequestParam(required = false) Double height,
                                          @RequestParam(required = false) Double weight,
                                          @RequestParam(required = false) Integer age,
                                          @RequestParam(required = false) String gender) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.updateUserInfo(userId, height, weight, age, gender);
        if (user != null) {
            result.put("success", true);
            result.put("user", user);
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        return result;
    }

    // 更新目标体重
    @PostMapping("/updateTargetWeight")
    public Map<String, Object> updateTargetWeight(
            @RequestParam Integer userId,
            @RequestParam Double targetWeight) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.updateTargetWeight(userId, targetWeight);
        if (user != null) {
            result.put("success", true);
            result.put("message", "目标体重设置成功");
            result.put("targetWeight", targetWeight);
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        return result;
    }

    // 获取用户完整信息（包含统计数据）
    @GetMapping("/profile/{userId}")
    public Map<String, Object> getUserProfile(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.getUserById(userId);

        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        int postCount = postRepository.countByUserId(userId);
        int followerCount = followService.getFollowerCount(userId);
        int followingCount = followService.getFollowingCount(userId);

        result.put("success", true);
        result.put("user", user);
        result.put("postCount", postCount);
        result.put("followerCount", followerCount);
        result.put("followingCount", followingCount);
        return result;
    }

    // 更新个人信息
    @PostMapping("/updateProfile")
    public Map<String, Object> updateProfile(
            @RequestParam Integer userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer age) {

        Map<String, Object> result = new HashMap<>();
        User user = userService.getUserById(userId);

        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        if (age != null) {
            user.setAge(age);
        }

        userRepository.save(user);

        result.put("success", true);
        result.put("message", "个人信息更新成功");
        result.put("user", user);
        return result;
    }

    @PostMapping("/uploadAvatar")
    public Map<String, Object> uploadAvatar(
            @RequestParam Integer userId,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false) MultipartFile file) {

        Map<String, Object> result = new HashMap<>();
        User user = userService.getUserById(userId);

        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        try {
            String savedPath = null;

            if (file != null && !file.isEmpty()) {
                savedPath = saveAvatarFile(file, userId);
            }
            else if (avatarUrl != null && !avatarUrl.isEmpty() && avatarUrl.startsWith("http")) {
                savedPath = downloadAndSaveImage(avatarUrl, userId);
            }
            else if (avatarUrl != null && !avatarUrl.isEmpty()) {
                savedPath = saveAvatarBase64(avatarUrl, userId);
            }
            else {
                result.put("success", false);
                result.put("message", "请提供图片文件、Base64数据或图片URL");
                return result;
            }

            if (savedPath != null) {
                user.setAvatar(savedPath);
                userRepository.save(user);
                result.put("success", true);
                result.put("message", "头像更新成功");
                result.put("avatarUrl", savedPath);
            } else {
                result.put("success", false);
                result.put("message", "头像保存失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }

        return result;
    }

    private String saveAvatarFile(MultipartFile file, Integer userId) throws Exception {
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = System.currentTimeMillis() + "_" + userId + suffix;
        String filePath = uploadDir + fileName;

        file.transferTo(new File(filePath));
        return "/uploads/avatars/" + fileName;
    }

    private String downloadAndSaveImage(String imageUrl, Integer userId) throws Exception {
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        System.out.println("开始下载头像: " + imageUrl);

        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        System.out.println("响应码: " + responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                byte[] errorBytes = errorStream.readAllBytes();
                System.out.println("错误信息: " + new String(errorBytes));
            }
            throw new Exception("图片下载失败，HTTP状态码: " + responseCode);
        }

        String contentType = connection.getContentType();
        String suffix = ".png";
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                suffix = ".jpg";
            } else if (contentType.contains("png")) {
                suffix = ".png";
            } else if (contentType.contains("gif")) {
                suffix = ".gif";
            } else if (contentType.contains("webp")) {
                suffix = ".webp";
            } else if (contentType.contains("svg+xml")) {
                suffix = ".svg";
            }
        }

        String fileName = System.currentTimeMillis() + "_" + userId + suffix;
        String filePath = uploadDir + fileName;

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        connection.disconnect();
        System.out.println("头像保存成功: " + filePath);
        return "/uploads/avatars/" + fileName;
    }

    private String saveAvatarBase64(String base64Data, Integer userId) throws Exception {
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String base64Image = base64Data;
        if (base64Data.contains(",")) {
            base64Image = base64Data.split(",")[1];
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        String fileName = System.currentTimeMillis() + "_" + userId + ".png";
        String filePath = uploadDir + fileName;

        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(imageBytes);
        fos.close();

        return "/uploads/avatars/" + fileName;
    }

    @PostMapping("/changePassword")
    public Map<String, Object> changePassword(
            @RequestParam Integer userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        Map<String, Object> result = new HashMap<>();
        User user = userService.getUserById(userId);

        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            result.put("success", false);
            result.put("message", "原密码错误");
            return result;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        result.put("success", true);
        result.put("message", "密码修改成功，请重新登录");
        return result;
    }

    @PostMapping("/send-sms-code")
    public Map<String, Object> sendSmsCode(@RequestParam String phone) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("========== 短信验证码 ==========");
        System.out.println("手机号：" + phone);
        System.out.println("验证码：123456");
        System.out.println("================================");
        result.put("success", true);
        result.put("message", "验证码已发送（测试验证码：123456）");
        return result;
    }

    @PostMapping("/sms-login")
    public Map<String, Object> smsLogin(@RequestParam String phone,
                                        @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        if (!"123456".equals(code)) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }
        User user = userService.smsLogin(phone, code);
        if (user != null) {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("role", user.getRole());
        } else {
            result.put("success", false);
            result.put("message", "登录失败");
        }
        return result;
    }

    @PostMapping("/send-register-sms")
    public Map<String, Object> sendRegisterSms(@RequestParam String phone) {
        Map<String, Object> result = new HashMap<>();
        if (userService.existsByPhone(phone)) {
            result.put("success", false);
            result.put("message", "手机号已注册，请直接登录");
            return result;
        }
        System.out.println("========== 注册验证码 ==========");
        System.out.println("手机号：" + phone);
        System.out.println("验证码：123456");
        System.out.println("================================");
        result.put("success", true);
        result.put("message", "验证码已发送（测试验证码：123456）");
        return result;
    }

    @PostMapping("/phone-register")
    public Map<String, Object> phoneRegister(@RequestParam String phone,
                                             @RequestParam String code,
                                             @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        if (!"123456".equals(code)) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }
        if (userService.existsByPhone(phone)) {
            result.put("success", false);
            result.put("message", "手机号已注册");
            return result;
        }
        User user = userService.phoneRegister(phone, password);
        result.put("success", true);
        result.put("message", "注册成功");
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        return result;
    }

    @PostMapping("/send-email-code")
    public Map<String, Object> sendEmailCode(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("========== 邮箱验证码 ==========");
        System.out.println("邮箱：" + email);
        System.out.println("验证码：123456");
        System.out.println("================================");
        result.put("success", true);
        result.put("message", "验证码已发送（测试验证码：123456）");
        return result;
    }

    @PostMapping("/email-login")
    public Map<String, Object> emailLogin(@RequestParam String email,
                                          @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.emailLogin(email, password);
        if (user != null) {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("role", user.getRole());
        } else {
            result.put("success", false);
            result.put("message", "邮箱或密码错误");
        }
        return result;
    }

    @PostMapping("/email-code-login")
    public Map<String, Object> emailCodeLogin(@RequestParam String email,
                                              @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        if (!"123456".equals(code)) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }
        User user = userService.emailCodeLogin(email, code);
        if (user != null) {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("role", user.getRole());
        } else {
            result.put("success", false);
            result.put("message", "登录失败");
        }
        return result;
    }

    // ==================== 新增：绑定/解绑相关接口 ====================

    /**
     * 获取用户绑定信息（脱敏显示）
     */
    @GetMapping("/bind-info/{userId}")
    public Map<String, Object> getBindInfo(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.getUserById(userId);

        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        result.put("success", true);
        result.put("hasPhone", user.getPhone() != null && !user.getPhone().isEmpty());
        result.put("maskedPhone", userService.getMaskedPhone(userId));
        result.put("hasEmail", user.getEmail() != null && !user.getEmail().isEmpty());
        result.put("maskedEmail", userService.getMaskedEmail(userId));
        result.put("hasWechat", user.getOpenId() != null && !user.getOpenId().isEmpty());
        result.put("maskedWechat", userService.getMaskedWechat(userId));

        return result;
    }

    /**
     * 发送绑定手机号验证码
     */
    @PostMapping("/send-bind-sms")
    public Map<String, Object> sendBindSms(@RequestParam String phone) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("========== 绑定手机号验证码 ==========");
        System.out.println("手机号：" + phone);
        System.out.println("验证码：123456");
        System.out.println("=====================================");
        result.put("success", true);
        result.put("message", "验证码已发送（测试验证码：123456）");
        return result;
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bind-phone")
    public Map<String, Object> bindPhone(@RequestParam Integer userId,
                                         @RequestParam String phone,
                                         @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        String msg = userService.bindPhone(userId, phone, code);
        result.put("success", "绑定成功".equals(msg));
        result.put("message", msg);
        return result;
    }

    /**
     * 解绑手机号
     */
    @PostMapping("/unbind-phone")
    public Map<String, Object> unbindPhone(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        String msg = userService.unbindPhone(userId);
        result.put("success", "解绑成功".equals(msg));
        result.put("message", msg);
        return result;
    }

    /**
     * 发送绑定邮箱验证码
     */
    @PostMapping("/send-bind-email-code")
    public Map<String, Object> sendBindEmailCode(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("========== 绑定邮箱验证码 ==========");
        System.out.println("邮箱：" + email);
        System.out.println("验证码：123456");
        System.out.println("===================================");
        result.put("success", true);
        result.put("message", "验证码已发送（测试验证码：123456）");
        return result;
    }

    /**
     * 绑定邮箱
     */
    @PostMapping("/bind-email")
    public Map<String, Object> bindEmail(@RequestParam Integer userId,
                                         @RequestParam String email,
                                         @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        String msg = userService.bindEmail(userId, email, code);
        result.put("success", "绑定成功".equals(msg));
        result.put("message", msg);
        return result;
    }

    /**
     * 解绑邮箱
     */
    @PostMapping("/unbind-email")
    public Map<String, Object> unbindEmail(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        String msg = userService.unbindEmail(userId);
        result.put("success", "解绑成功".equals(msg));
        result.put("message", msg);
        return result;
    }

    // ==================== 新增：获取用户禁言状态 ====================

    /**
     * 获取用户禁言状态
     */
    @GetMapping("/{userId}/ban-status")
    public Map<String, Object> getUserBanStatus(@PathVariable Integer userId) {
        return communityService.getUserBanStatus(userId);
    }

    // ========== 获取用户每日营养目标（根据身体数据动态计算） ==========
    @GetMapping("/nutrition-goal/{userId}")
    public Map<String, Object> getNutritionGoal(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        User user = userService.getUserById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 获取用户数据
        Double height = user.getCurrentHeight();
        Double weight = user.getCurrentWeight();
        Integer age = user.getAge();
        String gender = user.getGender();
        String activityLevel = user.getActivityLevel();

        // 兼容数字性别：0=女, 1=男
        if (gender != null && "0".equals(gender)) {
            gender = "FEMALE";
        }
        if (gender != null && "1".equals(gender)) {
            gender = "MALE";
        }

        // 默认值（如果数据不完整）
        if (height == null || height <= 0) height = 170.0;
        if (weight == null || weight <= 0) weight = 65.0;
        if (age == null || age <= 0) age = 25;
        if (gender == null) gender = "MALE";

        // 1. 计算基础代谢率 BMR (Harris-Benedict 公式)
        double bmr;
        if ("FEMALE".equalsIgnoreCase(gender)) {
            bmr = 655 + (9.6 * weight) + (1.8 * height) - (4.7 * age);
        } else {
            bmr = 66 + (13.7 * weight) + (5 * height) - (6.8 * age);
        }

        // 2. 活动系数
        double activityFactor;
        if (activityLevel == null) {
            activityFactor = 1.55;
        } else {
            switch (activityLevel.toUpperCase()) {
                case "SEDENTARY": activityFactor = 1.2; break;
                case "LIGHT": activityFactor = 1.375; break;
                case "MODERATE": activityFactor = 1.55; break;
                case "ACTIVE": activityFactor = 1.725; break;
                case "VERY_ACTIVE": activityFactor = 1.9; break;
                default: activityFactor = 1.55;
            }
        }

        // 3. 每日总消耗 TDEE
        double tdee = bmr * activityFactor;

        // 4. 根据目标体重调整热量
        Double targetWeight = user.getTargetWeight();
        double calorieTarget = tdee;
        if (targetWeight != null && targetWeight > 0 && targetWeight < weight) {
            calorieTarget = tdee - 300;  // 减脂
        } else if (targetWeight != null && targetWeight > weight) {
            calorieTarget = tdee + 300;  // 增肌
        }

        calorieTarget = Math.max(calorieTarget, 1200);

        // 5. 计算营养分配
        double proteinGram = weight * 1.6;
        double proteinCalories = proteinGram * 4;
        double fatCalories = calorieTarget * 0.25;
        double fatGram = fatCalories / 9;
        double carbsCalories = calorieTarget - proteinCalories - fatCalories;
        double carbsGram = carbsCalories / 4;
        double fiberGram = (calorieTarget / 1000) * 14;
        double sugarGram = (calorieTarget * 0.1) / 4;

        result.put("success", true);
        result.put("calories", Math.round(calorieTarget));
        result.put("protein", Math.round(proteinGram * 10) / 10.0);
        result.put("fat", Math.round(fatGram * 10) / 10.0);
        result.put("carbs", Math.round(carbsGram * 10) / 10.0);
        result.put("fiber", Math.round(fiberGram * 10) / 10.0);
        result.put("sugar", Math.round(sugarGram * 10) / 10.0);
        result.put("sodium", 2000);
        result.put("calcium", 800);

        return result;
    }
}