package com.health.health_system.controller;

import com.alibaba.fastjson.JSONObject;
import com.health.health_system.entity.DietRecord;
import com.health.health_system.service.BaiduAIService;
import com.health.health_system.service.DietRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diet")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class DietRecordController {

    @Autowired
    private DietRecordService dietRecordService;

    @Autowired
    private BaiduAIService baiduAIService;

    // 添加饮食记录 - 支持营养字段
    @PostMapping("/add")
    public Map<String, Object> addDietRecord(
            @RequestParam Integer userId,
            @RequestParam String mealType,
            @RequestParam String foodName,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) Integer calories,
            @RequestParam(required = false) String recordDate,
            // ========== 新增 7 个营养参数 ==========
            @RequestParam(required = false) Double protein,
            @RequestParam(required = false) Double fat,
            @RequestParam(required = false) Double carbs,
            @RequestParam(required = false) Double fiber,
            @RequestParam(required = false) Double sugar,
            @RequestParam(required = false) Integer sodium,
            @RequestParam(required = false) Integer calcium) {

        Map<String, Object> result = new HashMap<>();
        LocalDate date = (recordDate != null) ? LocalDate.parse(recordDate) : LocalDate.now();

        DietRecord record = dietRecordService.addDietRecord(userId, mealType, foodName, quantity, calories, date,
                protein, fat, carbs, fiber, sugar, sodium, calcium);

        result.put("success", true);
        result.put("message", "添加成功");
        result.put("record", record);
        return result;
    }

    // 获取某一天的饮食记录
    @GetMapping("/daily/{userId}")
    public Map<String, Object> getDailyDiet(
            @PathVariable Integer userId,
            @RequestParam(required = false) String recordDate) {

        LocalDate date = (recordDate != null) ? LocalDate.parse(recordDate) : LocalDate.now();
        return dietRecordService.getDailySummary(userId, date);
    }

    // 删除饮食记录
    @DeleteMapping("/delete/{recordId}")
    public Map<String, Object> deleteRecord(
            @PathVariable Integer recordId,
            @RequestParam Integer userId) {

        Map<String, Object> result = new HashMap<>();
        boolean success = dietRecordService.deleteRecord(recordId, userId);

        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }

    // 上传图片识别食物（带日志）
    @PostMapping("/recognize")
    public Map<String, Object> recognizeFood(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("========== 开始识别食物 ==========");
        System.out.println("收到文件: " + file.getOriginalFilename());
        System.out.println("文件大小: " + file.getSize() + " bytes");

        try {
            byte[] imageBytes = file.getBytes();
            System.out.println("图片字节数: " + imageBytes.length);

            System.out.println("调用百度API识别菜品...");
            JSONObject aiResult = baiduAIService.recognizeDish(imageBytes);

            System.out.println("百度API返回结果: " + aiResult.toJSONString());

            JSONObject parsed = baiduAIService.parseDishResult(aiResult);

            result.put("success", parsed.getBoolean("success"));
            result.put("message", parsed.getString("message"));

            if (parsed.getBoolean("success")) {
                result.put("foodName", parsed.getString("foodName"));
                result.put("probability", parsed.getDouble("probability"));
                result.put("estimatedCalories", parsed.getInteger("estimatedCalories"));
                System.out.println("识别成功！食物: " + parsed.getString("foodName") + ", 热量: " + parsed.getInteger("estimatedCalories"));
            } else {
                System.out.println("识别失败: " + parsed.getString("message"));
            }

            System.out.println("========== 识别结束 ==========");

        } catch (Exception e) {
            System.err.println("识别异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "识别失败：" + e.getMessage());
        }

        return result;
    }
}