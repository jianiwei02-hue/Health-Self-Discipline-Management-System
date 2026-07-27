package com.health.health_system.service;

import com.health.health_system.entity.User;
import com.health.health_system.repository.UserRepository;
import com.health.health_system.utils.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoderUtil passwordEncoder;

    // 注册（密码加密）
    public String register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return "用户名已存在";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));  // 加密存储
        user.setRole("USER");
        user.setCreateTime(LocalDateTime.now());

        userRepository.save(user);
        return "注册成功";
    }

    // 登录（验证加密密码）
    public User login(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 验证密码
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    // 根据ID获取用户信息
    public User getUserById(Integer userId) {
        return userRepository.findById(userId).orElse(null);
    }

    // 更新用户信息
    public User updateUserInfo(Integer userId, Double height, Double weight, Integer age, String gender) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (height != null) user.setCurrentHeight(height);
            if (weight != null) user.setCurrentWeight(weight);
            if (age != null) user.setAge(age);
            if (gender != null) user.setGender(gender);
            return userRepository.save(user);
        }
        return null;
    }

    // 更新目标体重
    public User updateTargetWeight(Integer userId, Double targetWeight) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTargetWeight(targetWeight);
            return userRepository.save(user);
        }
        return null;
    }

    // 获取所有用户
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 删除用户
    public boolean deleteUser(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && !"ADMIN".equals(userOpt.get().getRole())) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    // ========== 新增登录方式方法 ==========

    /**
     * 短信验证码登录（手机号不存在则自动注册）
     */
    public User smsLogin(String phone, String code) {
        // 这里简化处理，实际需要验证 code
        Optional<User> userOpt = userRepository.findByPhone(phone);

        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 手机号不存在，自动创建新用户
        User newUser = new User();
        // 生成用户名：用"u" + 手机号后4位
        String username = "u" + phone.substring(phone.length() - 4);
        newUser.setUsername(username);
        newUser.setPhone(phone);
        newUser.setPassword(passwordEncoder.encode("123456")); // 默认密码
        newUser.setRole("USER");
        newUser.setNickname("用户" + phone.substring(phone.length() - 4));
        newUser.setCreateTime(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    /**
     * 邮箱密码登录
     */
    public User emailLogin(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    /**
     * 邮箱验证码登录（邮箱不存在则自动注册）
     */
    public User emailCodeLogin(String email, String code) {
        // 这里简化处理，实际需要验证 code
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 邮箱不存在，自动创建新用户
        User newUser = new User();
        String username = email.split("@")[0];  // 用邮箱@前面的部分作为用户名
        // 如果用户名已存在，加上随机数
        if (userRepository.existsByUsername(username)) {
            username = username + System.currentTimeMillis() % 10000;
        }
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode("123456")); // 默认密码
        newUser.setRole("USER");
        newUser.setNickname(username);
        newUser.setCreateTime(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    /**
     * 手机号注册
     */
    public User phoneRegister(String phone, String password) {
        User user = new User();
        String username = "u" + phone.substring(phone.length() - 4);
        // 如果用户名已存在，加上随机数
        if (userRepository.existsByUsername(username)) {
            username = username + System.currentTimeMillis() % 10000;
        }
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setCreateTime(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * 检查手机号是否已存在
     */
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    /**
     * 检查邮箱是否已存在
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ==================== 新增：绑定/解绑 相关方法 ====================

    /**
     * 绑定手机号
     * @param userId 用户ID
     * @param phone 手机号
     * @param code 验证码（实际需验证）
     * @return 绑定结果消息
     */
    public String bindPhone(Integer userId, String phone, String code) {
        // 1. 验证验证码（这里简化，实际需要从缓存中校验）
        // 假设正确验证码是 "123456"（测试用）
        if (!"123456".equals(code)) {
            return "验证码错误";
        }

        // 2. 检查手机号是否已被其他用户绑定
        Optional<User> existingUser = userRepository.findByPhone(phone);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            return "该手机号已被其他账号绑定";
        }

        // 3. 绑定手机号
        User user = getUserById(userId);
        if (user == null) {
            return "用户不存在";
        }

        user.setPhone(phone);
        userRepository.save(user);
        return "绑定成功";
    }

    /**
     * 解绑手机号
     * @param userId 用户ID
     * @return 解绑结果消息
     */
    public String unbindPhone(Integer userId) {
        User user = getUserById(userId);
        if (user == null) {
            return "用户不存在";
        }

        // 检查是否还有其他登录方式（用户名密码、邮箱、微信）
        boolean hasOtherLoginMethod = true;
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            hasOtherLoginMethod = false;
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            hasOtherLoginMethod = true;
        }
        if (user.getOpenId() != null && !user.getOpenId().isEmpty()) {
            hasOtherLoginMethod = true;
        }

        if (!hasOtherLoginMethod) {
            return "至少需要保留一种登录方式，请先绑定其他登录方式";
        }

        user.setPhone(null);
        userRepository.save(user);
        return "解绑成功";
    }

    /**
     * 绑定邮箱
     * @param userId 用户ID
     * @param email 邮箱
     * @param code 验证码
     * @return 绑定结果消息
     */
    public String bindEmail(Integer userId, String email, String code) {
        // 1. 验证验证码
        if (!"123456".equals(code)) {
            return "验证码错误";
        }

        // 2. 检查邮箱是否已被其他用户绑定
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            return "该邮箱已被其他账号绑定";
        }

        // 3. 绑定邮箱
        User user = getUserById(userId);
        if (user == null) {
            return "用户不存在";
        }

        user.setEmail(email);
        userRepository.save(user);
        return "绑定成功";
    }

    /**
     * 解绑邮箱
     * @param userId 用户ID
     * @return 解绑结果消息
     */
    public String unbindEmail(Integer userId) {
        User user = getUserById(userId);
        if (user == null) {
            return "用户不存在";
        }

        // 检查是否还有其他登录方式
        boolean hasOtherLoginMethod = true;
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            hasOtherLoginMethod = false;
        }
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            hasOtherLoginMethod = true;
        }
        if (user.getOpenId() != null && !user.getOpenId().isEmpty()) {
            hasOtherLoginMethod = true;
        }

        if (!hasOtherLoginMethod) {
            return "至少需要保留一种登录方式，请先绑定其他登录方式";
        }

        user.setEmail(null);
        userRepository.save(user);
        return "解绑成功";
    }

    /**
     * 获取用户已绑定的手机号（脱敏显示）
     * @param userId 用户ID
     * @return 脱敏后的手机号
     */
    public String getMaskedPhone(Integer userId) {
        User user = getUserById(userId);
        if (user == null || user.getPhone() == null) {
            return null;
        }
        String phone = user.getPhone();
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone.substring(0, Math.min(3, phone.length())) + "****";
    }

    /**
     * 获取用户已绑定的邮箱（脱敏显示）
     * @param userId 用户ID
     * @return 脱敏后的邮箱
     */
    public String getMaskedEmail(Integer userId) {
        User user = getUserById(userId);
        if (user == null || user.getEmail() == null) {
            return null;
        }
        String email = user.getEmail();
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String prefix = email.substring(0, atIndex);
            if (prefix.length() <= 2) {
                return prefix + "***" + email.substring(atIndex);
            }
            return prefix.substring(0, 2) + "***" + email.substring(atIndex);
        }
        return email;
    }

    /**
     * 获取用户绑定的微信（脱敏显示）
     * @param userId 用户ID
     * @return 脱敏后的openId或提示
     */
    public String getMaskedWechat(Integer userId) {
        User user = getUserById(userId);
        if (user == null || user.getOpenId() == null || user.getOpenId().isEmpty()) {
            return null;
        }
        // 微信openId通常较长，只显示前后各4位
        String openId = user.getOpenId();
        if (openId.length() > 8) {
            return openId.substring(0, 4) + "****" + openId.substring(openId.length() - 4);
        }
        return "已绑定";
    }

    /**
     * 检查用户是否绑定了手机号
     */
    public boolean hasPhone(Integer userId) {
        User user = getUserById(userId);
        return user != null && user.getPhone() != null && !user.getPhone().isEmpty();
    }

    /**
     * 检查用户是否绑定了邮箱
     */
    public boolean hasEmail(Integer userId) {
        User user = getUserById(userId);
        return user != null && user.getEmail() != null && !user.getEmail().isEmpty();
    }

    /**
     * 检查用户是否绑定了微信
     */
    public boolean hasWechat(Integer userId) {
        User user = getUserById(userId);
        return user != null && user.getOpenId() != null && !user.getOpenId().isEmpty();
    }
}