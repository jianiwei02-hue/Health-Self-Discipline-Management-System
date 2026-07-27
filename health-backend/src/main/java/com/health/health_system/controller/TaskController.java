package com.health.health_system.controller;

import com.health.health_system.entity.CheckInSummary;
import com.health.health_system.entity.HabitTask;
import com.health.health_system.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // 创建任务
    @PostMapping("/create")
    public Map<String, Object> createTask(
            @RequestParam Integer userId,
            @RequestParam String taskName,
            @RequestParam(defaultValue = "SPORT") String taskType,
            @RequestParam(required = false) String targetValue,
            @RequestParam(required = false) String recordDate) {

        Map<String, Object> result = new HashMap<>();
        LocalDate date = (recordDate != null) ? LocalDate.parse(recordDate) : LocalDate.now();

        HabitTask task = taskService.createTask(userId, taskName, taskType, targetValue, date);

        result.put("success", true);
        result.put("message", "任务创建成功");
        result.put("task", task);
        return result;
    }

    // 完成任务（打卡）
    @PostMapping("/complete")
    public Map<String, Object> completeTask(@RequestParam Integer taskId) {
        return taskService.completeTask(taskId);
    }

    // 修改任务（仅允许修改未完成的任务）
    @PutMapping("/update/{taskId}")
    public Map<String, Object> updateTask(
            @PathVariable Integer taskId,
            @RequestParam String taskName,
            @RequestParam String taskType) {

        Map<String, Object> result = new HashMap<>();
        HabitTask task = taskService.updateTask(taskId, taskName, taskType);

        if (task != null) {
            result.put("success", true);
            result.put("message", "任务修改成功");
            result.put("task", task);
        } else {
            result.put("success", false);
            result.put("message", "任务不存在或已完成无法修改");
        }
        return result;
    }

    // 获取今日任务列表
    @GetMapping("/today/{userId}")
    public List<HabitTask> getTodayTasks(@PathVariable Integer userId) {
        return taskService.getTodayTasks(userId);
    }

    // 获取打卡汇总信息
    @GetMapping("/summary/{userId}")
    public CheckInSummary getSummary(@PathVariable Integer userId) {
        return taskService.getCheckInSummary(userId);
    }

    // 获取用户连续打卡天数
    @GetMapping("/consecutive/{userId}")
    public int getConsecutiveDays(@PathVariable Integer userId) {
        return taskService.getConsecutiveDays(userId);
    }
}