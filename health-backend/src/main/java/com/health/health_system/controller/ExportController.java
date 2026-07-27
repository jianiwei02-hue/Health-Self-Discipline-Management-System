package com.health.health_system.controller;

import com.alibaba.excel.EasyExcel;
import com.health.health_system.entity.DietRecord;
import com.health.health_system.entity.HealthRecord;
import com.health.health_system.entity.SportRecord;
import com.health.health_system.repository.DietRecordRepository;
import com.health.health_system.repository.HealthAnalysisRepository;
import com.health.health_system.repository.SportRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
public class ExportController {

    @Autowired
    private HealthAnalysisRepository healthRecordRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private SportRecordRepository sportRecordRepository;

    // 导出健康数据（身高体重记录）
    @GetMapping("/health/{userId}")
    public void exportHealthData(@PathVariable Integer userId, HttpServletResponse response) throws IOException {
        List<HealthRecord> records = healthRecordRepository.findByUserIdOrderByRecordDateAsc(userId);

        List<HealthRecordExcel> excelList = new ArrayList<>();
        for (HealthRecord record : records) {
            HealthRecordExcel excel = new HealthRecordExcel();
            excel.setRecordDate(record.getRecordDate().toString());
            excel.setHeight(record.getHeight());
            excel.setWeight(record.getWeight());
            excel.setBmi(record.getBmi());
            excelList.add(excel);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("健康数据_" + LocalDate.now(), "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), HealthRecordExcel.class)
                .sheet("健康数据")
                .doWrite(excelList);
    }

    // 导出饮食记录
    @GetMapping("/diet/{userId}")
    public void exportDietData(@PathVariable Integer userId, HttpServletResponse response) throws IOException {
        List<DietRecord> records = dietRecordRepository.findByUserIdOrderByRecordDateDesc(userId);

        List<DietRecordExcel> excelList = new ArrayList<>();
        for (DietRecord record : records) {
            DietRecordExcel excel = new DietRecordExcel();
            excel.setRecordDate(record.getRecordDate().toString());
            excel.setMealType(getMealName(record.getMealType()));
            excel.setFoodName(record.getFoodName());
            excel.setQuantity(record.getQuantity());
            excel.setCalories(record.getCalories());
            excelList.add(excel);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("饮食记录_" + LocalDate.now(), "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), DietRecordExcel.class)
                .sheet("饮食记录")
                .doWrite(excelList);
    }

    // 导出运动记录
    @GetMapping("/sport/{userId}")
    public void exportSportData(@PathVariable Integer userId, HttpServletResponse response) throws IOException {
        List<SportRecord> records = sportRecordRepository.findByUserIdOrderByRecordDateDesc(userId);

        List<SportRecordExcel> excelList = new ArrayList<>();
        for (SportRecord record : records) {
            SportRecordExcel excel = new SportRecordExcel();
            excel.setRecordDate(record.getRecordDate().toString());
            excel.setSportName(record.getSportName());
            excel.setDuration(record.getDuration());
            excel.setCaloriesBurned(record.getCaloriesBurned());
            excelList.add(excel);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("运动记录_" + LocalDate.now(), "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), SportRecordExcel.class)
                .sheet("运动记录")
                .doWrite(excelList);
    }

    private String getMealName(String type) {
        switch (type) {
            case "BREAKFAST": return "早餐";
            case "LUNCH": return "午餐";
            case "DINNER": return "晚餐";
            case "SNACK": return "加餐";
            default: return type;
        }
    }

    // ========== Excel 导出实体类 ==========

    public static class HealthRecordExcel {
        private String recordDate;
        private Double height;
        private Double weight;
        private Double bmi;

        public String getRecordDate() { return recordDate; }
        public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
        public Double getHeight() { return height; }
        public void setHeight(Double height) { this.height = height; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public Double getBmi() { return bmi; }
        public void setBmi(Double bmi) { this.bmi = bmi; }
    }

    public static class DietRecordExcel {
        private String recordDate;
        private String mealType;
        private String foodName;
        private Integer quantity;
        private Integer calories;

        public String getRecordDate() { return recordDate; }
        public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
    }

    public static class SportRecordExcel {
        private String recordDate;
        private String sportName;
        private Integer duration;
        private Integer caloriesBurned;

        public String getRecordDate() { return recordDate; }
        public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
        public String getSportName() { return sportName; }
        public void setSportName(String sportName) { this.sportName = sportName; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public Integer getCaloriesBurned() { return caloriesBurned; }
        public void setCaloriesBurned(Integer caloriesBurned) { this.caloriesBurned = caloriesBurned; }
    }
}