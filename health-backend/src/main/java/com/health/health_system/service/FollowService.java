package com.health.health_system.service;

import com.health.health_system.entity.User;
import com.health.health_system.entity.UserFollow;
import com.health.health_system.repository.FollowRepository;
import com.health.health_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 关注用户
     */
    @Transactional
    public boolean follow(Integer followerId, Integer followedId) {
        // 不能关注自己
        if (followerId.equals(followedId)) {
            return false;
        }
        // 已经关注过了
        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return false;
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowedId(followedId);
        follow.setCreateTime(LocalDateTime.now());
        followRepository.save(follow);
        return true;
    }

    /**
     * 取消关注
     */
    @Transactional
    public boolean unfollow(Integer followerId, Integer followedId) {
        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
            return true;
        }
        return false;
    }

    /**
     * 检查是否已关注
     */
    public boolean isFollowing(Integer followerId, Integer followedId) {
        if (followerId == null || followedId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    /**
     * 获取粉丝数量
     */
    public int getFollowerCount(Integer userId) {
        return followRepository.countByFollowedId(userId);
    }

    /**
     * 获取关注数量
     */
    public int getFollowingCount(Integer userId) {
        return followRepository.countByFollowerId(userId);
    }

    /**
     * 获取粉丝列表（包含用户信息）
     */
    public List<Map<String, Object>> getFollowers(Integer userId) {
        List<UserFollow> follows = followRepository.findByFollowedId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (UserFollow follow : follows) {
            Optional<User> userOpt = userRepository.findById(follow.getFollowerId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> item = new HashMap<>();
                item.put("id", user.getId());
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 获取关注列表（包含用户信息）
     */
    public List<Map<String, Object>> getFollowing(Integer userId) {
        List<UserFollow> follows = followRepository.findByFollowerId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (UserFollow follow : follows) {
            Optional<User> userOpt = userRepository.findById(follow.getFollowedId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> item = new HashMap<>();
                item.put("id", user.getId());
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
                result.add(item);
            }
        }
        return result;
    }
}