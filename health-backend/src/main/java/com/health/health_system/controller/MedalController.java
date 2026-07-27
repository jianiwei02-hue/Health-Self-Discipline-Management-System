package com.health.health_system.controller;

import com.health.health_system.entity.MedalRule;
import com.health.health_system.entity.UserMedal;
import com.health.health_system.service.MedalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medal")
@CrossOrigin(origins = "*")
public class MedalController {

    @Autowired
    private MedalService medalService;

    // 获取用户的所有勋章
    @GetMapping("/user/{userId}")
    public List<UserMedal> getUserMedals(@PathVariable Integer userId) {
        return medalService.getUserMedals(userId);
    }

    // 获取所有勋章规则
    @GetMapping("/rules")
    public List<MedalRule> getAllMedalRules() {
        return medalService.getAllMedalRules();
    }
}