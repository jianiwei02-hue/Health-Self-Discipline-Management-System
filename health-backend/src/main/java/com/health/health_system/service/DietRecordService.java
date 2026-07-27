package com.health.health_system.service;

import com.health.health_system.entity.DietRecord;
import com.health.health_system.repository.DietRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DietRecordService {

    @Autowired
    private DietRecordRepository dietRecordRepository;

    // 添加饮食记录 - 支持营养字段
    public DietRecord addDietRecord(Integer userId, String mealType, String foodName,
                                    Integer quantity, Integer calories, LocalDate recordDate,
                                    Double protein, Double fat, Double carbs, Double fiber,
                                    Double sugar, Integer sodium, Integer calcium) {
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setMealType(mealType);
        record.setFoodName(foodName);
        record.setQuantity(quantity != null ? quantity : 100);
        record.setCalories(calories != null ? calories : 0);
        record.setRecordDate(recordDate);
        record.setCreateTime(LocalDateTime.now());

        // ========== 设置营养字段 ==========
        record.setProtein(protein != null ? protein : 0.0);
        record.setFat(fat != null ? fat : 0.0);
        record.setCarbs(carbs != null ? carbs : 0.0);
        record.setFiber(fiber != null ? fiber : 0.0);
        record.setSugar(sugar != null ? sugar : 0.0);
        record.setSodium(sodium != null ? sodium : 0);
        record.setCalcium(calcium != null ? calcium : 0);

        return dietRecordRepository.save(record);
    }

    // 获取用户某一天的饮食记录
    public List<DietRecord> getDailyDiet(Integer userId, LocalDate recordDate) {
        return dietRecordRepository.findByUserIdAndRecordDateOrderByMealType(userId, recordDate);
    }

    // 获取用户某一天的总热量
    public Map<String, Object> getDailySummary(Integer userId, LocalDate recordDate) {
        Map<String, Object> result = new HashMap<>();

        List<DietRecord> records = getDailyDiet(userId, recordDate);
        Integer totalCalories = dietRecordRepository.sumCaloriesByUserIdAndRecordDate(userId, recordDate);

        result.put("records", records);
        result.put("totalCalories", totalCalories != null ? totalCalories : 0);
        result.put("recordDate", recordDate);

        return result;
    }

    // 删除饮食记录
    public boolean deleteRecord(Integer recordId, Integer userId) {
        DietRecord record = dietRecordRepository.findById(recordId).orElse(null);
        if (record != null && record.getUserId().equals(userId)) {
            dietRecordRepository.deleteById(recordId);
            return true;
        }
        return false;
    }
}