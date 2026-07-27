package com.health.health_system.repository;

import com.health.health_system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // 根据用户名查找用户
    Optional<User> findByUsername(String username);

    // 检查用户名是否存在
    boolean existsByUsername(String username);

    // 根据手机号查找用户
    Optional<User> findByPhone(String phone);

    // 根据邮箱查找用户
    Optional<User> findByEmail(String email);

    // 根据微信openId查找用户
    Optional<User> findByOpenId(String openId);

    // 检查手机号是否存在
    boolean existsByPhone(String phone);

    // 检查邮箱是否存在
    boolean existsByEmail(String email);

    // ========== 分页搜索（用于管理员后台） ==========
    Page<User> findByUsernameContaining(String username, Pageable pageable);
}