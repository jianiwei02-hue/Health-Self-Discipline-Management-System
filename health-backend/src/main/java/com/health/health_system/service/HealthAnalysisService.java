package com.health.health_system.service;

import com.health.health_system.entity.*;
import com.health.health_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HealthAnalysisService {

    @Autowired
    private HealthAnalysisRepository healthRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SleepRecordRepository sleepRecordRepository;

    @Autowired
    private WaterRecordRepository waterRecordRepository;

    @Autowired
    private HealthWeeklyReportRepository healthWeeklyReportRepository;

    @Autowired
    private SportRecordRepository sportRecordRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private HabitTaskRepository habitTaskRepository;

    @Autowired
    private CheckInSummaryRepository checkInSummaryRepository;

    // ========== 原有方法：记录健康数据 ==========
    public HealthRecord addHealthRecord(Integer userId, Double height, Double weight,
                                        Double bodyFat, Double chest, Double waist,
                                        Double hip, Double thigh, Integer restingHeartRate,
                                        Double sleepDuration, Integer waterIntake,
                                        LocalDate recordDate) {
        Double bmi = null;
        if (height != null && weight != null && height > 0) {
            double heightInMeters = height / 100;
            bmi = weight / (heightInMeters * heightInMeters);
            bmi = Math.round(bmi * 10) / 10.0;
        }

        HealthRecord existing = healthRecordRepository
                .findByUserIdAndRecordDate(userId, recordDate)
                .orElse(null);

        HealthRecord record;
        if (existing != null) {
            record = existing;
        } else {
            record = new HealthRecord();
            record.setUserId(userId);
            record.setRecordDate(recordDate);
            record.setCreateTime(LocalDateTime.now());
        }

        if (height != null) record.setHeight(height);
        if (weight != null) record.setWeight(weight);
        record.setBmi(bmi);
        if (bodyFat != null) record.setBodyFat(bodyFat);
        if (chest != null) record.setChest(chest);
        if (waist != null) record.setWaist(waist);
        if (hip != null) record.setHip(hip);
        if (thigh != null) record.setThigh(thigh);
        if (restingHeartRate != null) record.setRestingHeartRate(restingHeartRate);
        if (sleepDuration != null) record.setSleepDuration(sleepDuration);
        if (waterIntake != null) record.setWaterIntake(waterIntake);

        HealthRecord savedRecord = healthRecordRepository.save(record);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            if (height != null) user.setCurrentHeight(height);
            if (weight != null) user.setCurrentWeight(weight);
            if (bodyFat != null) user.setBodyFat(bodyFat);
            if (chest != null) user.setChest(chest);
            if (waist != null) user.setWaist(waist);
            if (hip != null) user.setHip(hip);
            if (thigh != null) user.setThigh(thigh);
            if (restingHeartRate != null) user.setRestingHeartRate(restingHeartRate);
            if (sleepDuration != null) user.setSleepDuration(sleepDuration);
            if (waterIntake != null) user.setWaterIntake(waterIntake);
            userRepository.save(user);
        }

        return savedRecord;
    }

    public List<HealthRecord> getUserHealthRecords(Integer userId) {
        return healthRecordRepository.findByUserIdOrderByRecordDateAsc(userId);
    }

    public Map<String, Object> getCurrentHealthStatus(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return result;
        }
        User user = userOpt.get();
        Double height = user.getCurrentHeight();
        Double weight = user.getCurrentWeight();
        Double bodyFat = user.getBodyFat();

        if (height != null && weight != null) {
            double heightM = height / 100;
            double bmi = weight / (heightM * heightM);
            result.put("bmi", Math.round(bmi * 10) / 10.0);
            result.put("height", height);
            result.put("weight", weight);
            String bmiStatus;
            if (bmi < 18.5) bmiStatus = "偏瘦";
            else if (bmi < 24) bmiStatus = "正常";
            else if (bmi < 28) bmiStatus = "偏重";
            else bmiStatus = "肥胖";
            result.put("bmiStatus", bmiStatus);
        }
        result.put("bodyFat", bodyFat);
        result.put("targetWeight", user.getTargetWeight());
        result.put("targetBodyFat", user.getTargetBodyFat());
        return result;
    }

    public Map<String, Object> getTrendData(Integer userId, String period, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        if ("week".equals(period)) {
            startDate = today.minusDays(6);
            endDate = today;
        } else if ("month".equals(period)) {
            startDate = today.minusDays(29);
            endDate = today;
        } else if ("year".equals(period)) {
            startDate = today.minusDays(364);
            endDate = today;
        }
        if (startDate == null) startDate = today.minusDays(29);
        if (endDate == null) endDate = today;

        List<HealthRecord> healthRecords = healthRecordRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);
        List<String> dates = new ArrayList<>();
        List<Double> bmiList = new ArrayList<>();
        List<Double> weightList = new ArrayList<>();
        List<Double> sleepList = new ArrayList<>();
        List<Integer> waterList = new ArrayList<>();

        // ========== 新增：围度和心率数据 ==========
        List<Double> chestList = new ArrayList<>();
        List<Double> waistList = new ArrayList<>();
        List<Double> hipList = new ArrayList<>();
        List<Double> thighList = new ArrayList<>();
        List<Integer> heartRateList = new ArrayList<>();

        Map<LocalDate, HealthRecord> healthMap = new HashMap<>();
        for (HealthRecord hr : healthRecords) {
            healthMap.put(hr.getRecordDate(), hr);
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.format(DateTimeFormatter.ofPattern("MM/dd")));
            HealthRecord hr = healthMap.get(current);
            if (hr != null) {
                bmiList.add(hr.getBmi());
                weightList.add(hr.getWeight());
                sleepList.add(hr.getSleepDuration());
                waterList.add(hr.getWaterIntake());
                // ========== 新增：围度和心率数据 ==========
                chestList.add(hr.getChest());
                waistList.add(hr.getWaist());
                hipList.add(hr.getHip());
                thighList.add(hr.getThigh());
                heartRateList.add(hr.getRestingHeartRate());
            } else {
                bmiList.add(null);
                weightList.add(null);
                sleepList.add(null);
                waterList.add(null);
                // ========== 新增：围度和心率数据 ==========
                chestList.add(null);
                waistList.add(null);
                hipList.add(null);
                thighList.add(null);
                heartRateList.add(null);
            }
            current = current.plusDays(1);
        }
        result.put("dates", dates);
        result.put("bmiTrend", bmiList);
        result.put("weightTrend", weightList);
        result.put("sleepTrend", sleepList);
        result.put("waterTrend", waterList);

        // ========== 新增：围度和心率数据 ==========
        result.put("chestTrend", chestList);
        result.put("waistTrend", waistList);
        result.put("hipTrend", hipList);
        result.put("thighTrend", thighList);
        result.put("heartRateTrend", heartRateList);

        return result;
    }

    public Map<String, Object> getCustomReport(Integer userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        List<HealthRecord> records = healthRecordRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);
        if (records.isEmpty()) {
            result.put("message", "该时间段无数据");
            return result;
        }
        HealthRecord firstRecord = records.get(0);
        HealthRecord lastRecord = records.get(records.size() - 1);
        Double weightChange = null;
        if (firstRecord.getWeight() != null && lastRecord.getWeight() != null) {
            weightChange = Math.round((lastRecord.getWeight() - firstRecord.getWeight()) * 10) / 10.0;
        }
        result.put("weightChange", weightChange);

        double sleepSum = 0;
        int sleepCount = 0;
        for (HealthRecord hr : records) {
            if (hr.getSleepDuration() != null) {
                sleepSum += hr.getSleepDuration();
                sleepCount++;
            }
        }
        double avgSleep = sleepCount > 0 ? Math.round((sleepSum / sleepCount) * 10) / 10.0 : 0;
        result.put("avgSleep", avgSleep);

        int totalWater = 0;
        for (HealthRecord hr : records) {
            if (hr.getWaterIntake() != null) {
                totalWater += hr.getWaterIntake();
            }
        }
        result.put("totalWater", totalWater);

        Double bodyFatChange = null;
        if (firstRecord.getBodyFat() != null && lastRecord.getBodyFat() != null) {
            bodyFatChange = Math.round((lastRecord.getBodyFat() - firstRecord.getBodyFat()) * 10) / 10.0;
        }
        result.put("bodyFatChange", bodyFatChange);

        String advice = generateAdvice(weightChange, avgSleep, totalWater, records.size());
        result.put("advice", advice);
        return result;
    }

    public List<String> getHealthTips(Integer userId) {
        List<String> tips = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);
        List<HealthRecord> records = healthRecordRepository.findByUserIdAndRecordDateBetween(userId, sevenDaysAgo, today);
        double sleepSum = 0;
        int sleepCount = 0;
        int waterSum = 0;
        int waterCount = 0;

        for (HealthRecord hr : records) {
            if (hr.getSleepDuration() != null) {
                sleepSum += hr.getSleepDuration();
                sleepCount++;
            }
            if (hr.getWaterIntake() != null) {
                waterSum += hr.getWaterIntake();
                waterCount++;
            }
        }
        double avgSleep = sleepCount > 0 ? sleepSum / sleepCount : 0;
        int avgWater = waterCount > 0 ? waterSum / waterCount : 0;

        if (avgSleep > 0 && avgSleep < 7.0) {
            tips.add("⚠️ 最近一周睡眠不足7小时，建议每晚提前30分钟入睡");
        } else if (avgSleep >= 8) {
            tips.add("😴 睡眠充足！保持这个好习惯");
        }
        if (avgWater > 0 && avgWater < 1500) {
            tips.add("💧 最近饮水量不足，每天建议喝1.5-2升水");
        } else if (avgWater >= 2000) {
            tips.add("👍 饮水量达标！继续保持");
        }
        if (tips.isEmpty()) {
            tips.add("👍 最近一周健康习惯保持得很好！继续加油");
            tips.add("🥗 多吃蔬菜水果，保持营养均衡");
            tips.add("🏃 每周保持3-5次运动，让身体更有活力");
        }
        return tips;
    }

    private String generateAdvice(Double weightChange, Double avgSleep, Integer totalWater, int recordCount) {
        List<String> adviceList = new ArrayList<>();
        if (weightChange != null) {
            if (weightChange > 0) {
                adviceList.add("体重增加了" + weightChange + "kg，建议增加运动量");
            } else if (weightChange < -0.5) {
                adviceList.add("体重控制得很好！继续保持");
            } else {
                adviceList.add("体重保持稳定");
            }
        }
        if (avgSleep != null && avgSleep < 7) {
            adviceList.add("睡眠不足，建议调整作息");
        } else if (avgSleep != null && avgSleep >= 8) {
            adviceList.add("睡眠充足，精力充沛");
        }
        if (totalWater != null && totalWater / recordCount < 1500) {
            adviceList.add("饮水量偏少，记得多喝水");
        }
        if (adviceList.isEmpty()) {
            return "各项指标良好，继续保持健康生活习惯！";
        }
        return String.join("；", adviceList);
    }

    // ========== 新增方法：获取热量平衡数据 ==========
    public Map<String, Object> getCalorieBalance(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        List<DietRecord> dietRecords = dietRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        int totalIntake = 0;
        for (DietRecord dr : dietRecords) {
            totalIntake += dr.getCalories() != null ? dr.getCalories() : 0;
        }
        List<SportRecord> sportRecords = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        int totalBurn = 0;
        for (SportRecord sr : sportRecords) {
            totalBurn += sr.getCaloriesBurned() != null ? sr.getCaloriesBurned() : 0;
        }
        int netBalance = totalIntake - totalBurn;
        String status;
        if (netBalance < -500) status = "快速减脂";
        else if (netBalance < 0) status = "温和减脂";
        else if (netBalance <= 100) status = "体重维持";
        else if (netBalance <= 500) status = "轻微增重";
        else status = "明显增重";
        result.put("totalIntake", totalIntake);
        result.put("totalBurn", totalBurn);
        result.put("netBalance", netBalance);
        result.put("status", status);
        return result;
    }

    // ========== 新增方法：获取运动分析数据 ==========
    public Map<String, Object> getSportAnalysis(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        LocalDate twoWeeksAgo = today.minusDays(13);
        List<SportRecord> thisWeekRecords = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        int weeklyCount = thisWeekRecords.size();
        int totalCalories = 0;
        Map<String, Integer> sportCount = new HashMap<>();
        for (SportRecord sr : thisWeekRecords) {
            totalCalories += sr.getCaloriesBurned() != null ? sr.getCaloriesBurned() : 0;
            sportCount.put(sr.getSportName(), sportCount.getOrDefault(sr.getSportName(), 0) + 1);
        }
        String favoriteSport = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : sportCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favoriteSport = entry.getKey();
            }
        }
        List<SportRecord> lastWeekRecords = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, twoWeeksAgo, weekAgo.minusDays(1));
        int lastWeekCount = lastWeekRecords.size();
        int lastWeekCalories = 0;
        for (SportRecord sr : lastWeekRecords) {
            lastWeekCalories += sr.getCaloriesBurned() != null ? sr.getCaloriesBurned() : 0;
        }
        result.put("weeklyCount", weeklyCount);
        result.put("weeklyChange", weeklyCount - lastWeekCount);
        result.put("totalCalories", totalCalories);
        result.put("caloriesChange", totalCalories - lastWeekCalories);
        result.put("favoriteSport", favoriteSport.isEmpty() ? "暂无" : favoriteSport);
        return result;
    }

    // ========== 新增方法：获取营养分析数据 ==========
    public Map<String, Object> getNutritionAnalysis(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        List<DietRecord> dietRecords = dietRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        double totalProtein = 0, totalFat = 0, totalCarbs = 0, totalFiber = 0;
        for (DietRecord dr : dietRecords) {
            totalProtein += dr.getProtein() != null ? dr.getProtein() : 0;
            totalFat += dr.getFat() != null ? dr.getFat() : 0;
            totalCarbs += dr.getCarbs() != null ? dr.getCarbs() : 0;
            totalFiber += dr.getFiber() != null ? dr.getFiber() : 0;
        }
        double proteinCalories = totalProtein * 4;
        double fatCalories = totalFat * 9;
        double carbsCalories = totalCarbs * 4;
        double totalCaloriesFromNutrients = proteinCalories + fatCalories + carbsCalories;
        double proteinPercent = totalCaloriesFromNutrients > 0 ? Math.round((proteinCalories / totalCaloriesFromNutrients) * 100) : 0;
        double fatPercent = totalCaloriesFromNutrients > 0 ? Math.round((fatCalories / totalCaloriesFromNutrients) * 100) : 0;
        double carbsPercent = totalCaloriesFromNutrients > 0 ? Math.round((carbsCalories / totalCaloriesFromNutrients) * 100) : 0;
        result.put("protein", Math.round(totalProtein * 10) / 10.0);
        result.put("fat", Math.round(totalFat * 10) / 10.0);
        result.put("carbs", Math.round(totalCarbs * 10) / 10.0);
        result.put("fiber", Math.round(totalFiber * 10) / 10.0);
        result.put("proteinPercent", proteinPercent);
        result.put("fatPercent", fatPercent);
        result.put("carbsPercent", carbsPercent);
        return result;
    }

    // ========== 新增方法：获取打卡习惯分析 ==========
    public Map<String, Object> getHabitAnalysis(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        CheckInSummary summary = checkInSummaryRepository.findByUserId(userId).orElse(null);
        int consecutiveDays = summary != null ? summary.getConsecutiveDays() : 0;
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        List<HabitTask> thisWeekTasks = habitTaskRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        int totalTasks = thisWeekTasks.size();
        int completedTasks = 0;
        for (HabitTask task : thisWeekTasks) {
            if (task.getStatus() == 1) completedTasks++;
        }
        int weeklyCompletion = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;
        result.put("consecutiveDays", consecutiveDays);
        result.put("weeklyCompletion", weeklyCompletion);
        return result;
    }

    // ========== 新增方法：获取智能建议汇总 ==========
    public List<String> getSmartInsights(Integer userId) {
        List<String> insights = new ArrayList<>();
        Map<String, Object> calorieBalance = getCalorieBalance(userId);
        int netBalance = (int) calorieBalance.get("netBalance");
        if (netBalance > 500) {
            insights.add("🍔 热量盈余 " + netBalance + " 大卡，建议增加运动或减少高热量食物");
        } else if (netBalance < -500) {
            insights.add("🔥 热量赤字 " + (-netBalance) + " 大卡，减脂效果明显，注意不要过度节食");
        } else if (netBalance > 0 && netBalance <= 500) {
            insights.add("⚖️ 热量轻微盈余，体重可能缓慢增加");
        }
        Map<String, Object> sportAnalysis = getSportAnalysis(userId);
        int weeklyCount = (int) sportAnalysis.get("weeklyCount");
        if (weeklyCount < 3) {
            insights.add("🏃 本周运动 " + weeklyCount + " 次，建议增加到每周 3-5 次");
        } else if (weeklyCount >= 5) {
            insights.add("💪 运动达人！本周已运动 " + weeklyCount + " 次，继续保持");
        }
        Map<String, Object> nutritionAnalysis = getNutritionAnalysis(userId);
        double protein = (double) nutritionAnalysis.get("protein");
        if (protein < 50) {
            insights.add("🥩 蛋白质摄入不足，建议增加鸡胸肉、鸡蛋、豆制品");
        }
        Map<String, Object> habitAnalysis = getHabitAnalysis(userId);
        int consecutiveDays = (int) habitAnalysis.get("consecutiveDays");
        if (consecutiveDays >= 7) {
            insights.add("🎉 连续打卡 " + consecutiveDays + " 天！自律成就即将达成");
        } else if (consecutiveDays >= 3) {
            insights.add("🌟 已连续打卡 " + consecutiveDays + " 天，再接再厉");
        } else if (consecutiveDays == 0) {
            insights.add("🌱 今天开始打卡吧！从小目标开始，逐步养成好习惯");
        }
        if (insights.isEmpty()) {
            insights.add("👍 各项指标良好，继续保持健康生活习惯！");
        }
        return insights;
    }

    // ========== 新增方法：获取体重与运动关联趋势 ==========
    public Map<String, Object> getCorrelationTrend(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(13);
        List<HealthRecord> healthRecords = healthRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        List<SportRecord> sportRecords = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        Map<LocalDate, Double> weightMap = new HashMap<>();
        for (HealthRecord hr : healthRecords) {
            if (hr.getWeight() != null) {
                weightMap.put(hr.getRecordDate(), hr.getWeight());
            }
        }
        Map<LocalDate, Integer> caloriesMap = new HashMap<>();
        for (SportRecord sr : sportRecords) {
            caloriesMap.put(sr.getRecordDate(), caloriesMap.getOrDefault(sr.getRecordDate(), 0) + (sr.getCaloriesBurned() != null ? sr.getCaloriesBurned() : 0));
        }
        List<String> dates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        List<Integer> calories = new ArrayList<>();
        LocalDate current = weekAgo;
        while (!current.isAfter(today)) {
            dates.add(current.format(DateTimeFormatter.ofPattern("MM/dd")));
            weights.add(weightMap.getOrDefault(current, null));
            calories.add(caloriesMap.getOrDefault(current, 0));
            current = current.plusDays(1);
        }
        result.put("dates", dates);
        result.put("weights", weights);
        result.put("calories", calories);
        return result;
    }

    // ========== 新增方法：获取健康综合评分 ==========
    public Map<String, Object> getHealthScore(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        int totalScore = 0;
        Map<String, Object> details = new HashMap<>();

        // 1. BMI评分 (25分)
        double bmiScore = 0;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getCurrentHeight() != null && user.getCurrentWeight() != null) {
            double heightM = user.getCurrentHeight() / 100;
            double bmi = user.getCurrentWeight() / (heightM * heightM);
            if (bmi >= 18.5 && bmi <= 24) {
                bmiScore = 25;
            } else if (bmi < 18.5) {
                bmiScore = Math.max(0, 25 - (18.5 - bmi) * 5);
            } else {
                bmiScore = Math.max(0, 25 - (bmi - 24) * 3);
            }
            details.put("bmi", Math.round(bmi * 10) / 10.0);
            details.put("bmiScore", Math.round(bmiScore));
        } else {
            details.put("bmi", "暂无");
            details.put("bmiScore", 0);
        }
        totalScore += bmiScore;

        // 2. 运动评分 (25分) - 本周运动次数
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        List<SportRecord> sportRecords = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        int sportCount = sportRecords.size();
        double sportScore = Math.min(25, sportCount * 5);
        details.put("sportCount", sportCount);
        details.put("sportScore", Math.round(sportScore));
        totalScore += sportScore;

        // 3. 饮食评分 (20分) - 本周平均蛋白质摄入
        List<DietRecord> dietRecords = dietRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        double totalProtein = 0;
        int dietDays = 0;
        for (DietRecord dr : dietRecords) {
            if (dr.getProtein() != null) {
                totalProtein += dr.getProtein();
                dietDays++;
            }
        }
        double avgProtein = dietDays > 0 ? totalProtein / dietDays : 0;
        double dietScore = Math.min(20, avgProtein / 50 * 20);
        details.put("avgProtein", Math.round(avgProtein * 10) / 10.0);
        details.put("dietScore", Math.round(dietScore));
        totalScore += dietScore;

        // 4. 睡眠评分 (15分) - 最近7天平均睡眠
        List<HealthRecord> healthRecords = healthRecordRepository.findByUserIdAndRecordDateBetween(userId, weekAgo, today);
        double totalSleep = 0;
        int sleepDays = 0;
        for (HealthRecord hr : healthRecords) {
            if (hr.getSleepDuration() != null) {
                totalSleep += hr.getSleepDuration();
                sleepDays++;
            }
        }
        double avgSleep = sleepDays > 0 ? totalSleep / sleepDays : 0;
        double sleepScore = Math.min(15, (avgSleep / 7) * 15);
        details.put("avgSleep", Math.round(avgSleep * 10) / 10.0);
        details.put("sleepScore", Math.round(sleepScore));
        totalScore += sleepScore;

        // 5. 打卡评分 (15分) - 连续打卡天数
        CheckInSummary summary = checkInSummaryRepository.findByUserId(userId).orElse(null);
        int consecutiveDays = summary != null ? summary.getConsecutiveDays() : 0;
        double checkScore = Math.min(15, consecutiveDays);
        details.put("consecutiveDays", consecutiveDays);
        details.put("checkScore", Math.round(checkScore));
        totalScore += checkScore;

        // 等级评定
        String level;
        if (totalScore >= 90) level = "优秀";
        else if (totalScore >= 75) level = "良好";
        else if (totalScore >= 60) level = "合格";
        else level = "待加强";

        result.put("success", true);
        result.put("totalScore", Math.round(totalScore));
        result.put("level", level);
        result.put("details", details);
        return result;
    }
}