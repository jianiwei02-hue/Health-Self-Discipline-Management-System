package com.health.health_system.controller;

import com.health.health_system.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@CrossOrigin(origins = "*")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 关注用户
     */
    @PostMapping("/follow")
    public Map<String, Object> follow(@RequestParam Integer followerId, @RequestParam Integer followedId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = followService.follow(followerId, followedId);
        result.put("success", success);
        result.put("message", success ? "关注成功" : "关注失败");
        return result;
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/unfollow")
    public Map<String, Object> unfollow(@RequestParam Integer followerId, @RequestParam Integer followedId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = followService.unfollow(followerId, followedId);
        result.put("success", success);
        result.put("message", success ? "取消关注成功" : "取消关注失败");
        return result;
    }

    /**
     * 获取关注状态
     */
    @GetMapping("/status")
    public Map<String, Object> getFollowStatus(@RequestParam Integer followerId, @RequestParam Integer followedId) {
        Map<String, Object> result = new HashMap<>();
        result.put("isFollowing", followService.isFollowing(followerId, followedId));
        result.put("followerCount", followService.getFollowerCount(followedId));
        result.put("followingCount", followService.getFollowingCount(followerId));
        return result;
    }

    /**
     * 获取粉丝列表
     */
    @GetMapping("/followers/{userId}")
    public List<Map<String, Object>> getFollowers(@PathVariable Integer userId) {
        return followService.getFollowers(userId);
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/following/{userId}")
    public List<Map<String, Object>> getFollowing(@PathVariable Integer userId) {
        return followService.getFollowing(userId);
    }
}