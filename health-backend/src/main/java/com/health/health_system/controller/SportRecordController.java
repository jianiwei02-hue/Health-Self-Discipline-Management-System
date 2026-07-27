package com.health.health_system.controller;

import com.health.health_system.entity.SportRecord;
import com.health.health_system.service.SportRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/sport")
@CrossOrigin(origins = "*")
public class SportRecordController {

    @Autowired
    private SportRecordService sportRecordService;

    /**
     * 添加运动记录（自动计算热量）
     */
    @PostMapping("/add")
    public Map<String, Object> addSportRecord(
            @RequestParam Integer userId,
            @RequestParam String sportName,
            @RequestParam Integer duration,
            @RequestParam(required = false) Integer caloriesBurned,
            @RequestParam(required = false) String recordDate) {

        Map<String, Object> result = new HashMap<>();
        LocalDate date = (recordDate != null) ? LocalDate.parse(recordDate) : LocalDate.now();

        try {
            // 调用 Service 添加记录（自动计算热量）
            SportRecord record = sportRecordService.addSportRecord(userId, sportName, duration, caloriesBurned, date);

            result.put("success", true);
            result.put("message", "添加成功");
            result.put("record", record);

            // 返回计算方式说明
            if (caloriesBurned != null && caloriesBurned > 0) {
                result.put("note", "使用用户手动输入的热量值");
            } else {
                result.put("note", "热量已自动计算（基于MET × 体重 × 0.0175 × 时长）");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 获取某一天的运动记录
     */
    @GetMapping("/daily/{userId}")
    public Map<String, Object> getDailySport(
            @PathVariable Integer userId,
            @RequestParam(required = false) String recordDate) {

        LocalDate date = (recordDate != null) ? LocalDate.parse(recordDate) : LocalDate.now();
        return sportRecordService.getDailySummary(userId, date);
    }

    /**
     * 获取所有运动记录
     */
    @GetMapping("/all/{userId}")
    public List<SportRecord> getAllSportRecords(@PathVariable Integer userId) {
        return sportRecordService.getAllSportRecords(userId);
    }

    /**
     * 获取用户的所有运动记录（别名接口，供肌肉热力图使用）
     */
    @GetMapping("/records/{userId}")
    public List<SportRecord> getUserSportRecords(@PathVariable Integer userId) {
        return sportRecordService.getUserSportRecords(userId);
    }

    /**
     * 删除运动记录
     */
    @DeleteMapping("/delete/{recordId}")
    public Map<String, Object> deleteRecord(
            @PathVariable Integer recordId,
            @RequestParam Integer userId) {

        Map<String, Object> result = new HashMap<>();

        boolean deleted = sportRecordService.deleteRecord(recordId, userId);

        if (deleted) {
            result.put("success", true);
            result.put("message", "删除成功");
        } else {
            result.put("success", false);
            result.put("message", "记录不存在或无权限删除");
        }
        return result;
    }

    /**
     * 更新运动记录（自动重新计算热量）
     */
    @PutMapping("/update/{recordId}")
    public Map<String, Object> updateSportRecord(
            @PathVariable Integer recordId,
            @RequestParam Integer userId,
            @RequestParam String sportName,
            @RequestParam Integer duration,
            @RequestParam(required = false) Integer caloriesBurned) {

        Map<String, Object> result = new HashMap<>();

        SportRecord record = sportRecordService.updateSportRecord(recordId, userId, sportName, duration, caloriesBurned);

        if (record != null) {
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("record", record);
            if (caloriesBurned != null && caloriesBurned > 0) {
                result.put("note", "使用用户手动输入的热量值");
            } else {
                result.put("note", "热量已自动重新计算");
            }
        } else {
            result.put("success", false);
            result.put("message", "记录不存在或无权限修改");
        }
        return result;
    }

    /**
     * 获取运动消耗统计（用于图表）
     */
    @GetMapping("/stats/{userId}")
    public Map<String, Object> getSportStats(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "week") String period) {

        return sportRecordService.getSportStats(userId, period);
    }
}