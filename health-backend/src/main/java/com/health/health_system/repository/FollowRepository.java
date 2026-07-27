package com.health.health_system.repository;

import com.health.health_system.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<UserFollow, Integer> {

    // 检查是否已关注
    boolean existsByFollowerIdAndFollowedId(Integer followerId, Integer followedId);

    // 取消关注
    void deleteByFollowerIdAndFollowedId(Integer followerId, Integer followedId);

    // 获取粉丝列表
    List<UserFollow> findByFollowedId(Integer followedId);

    // 获取关注列表
    List<UserFollow> findByFollowerId(Integer followerId);

    // 获取粉丝数量
    int countByFollowedId(Integer followedId);

    // 获取关注数量
    int countByFollowerId(Integer followerId);
}