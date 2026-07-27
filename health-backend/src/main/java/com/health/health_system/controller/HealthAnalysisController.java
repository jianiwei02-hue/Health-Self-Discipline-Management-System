package com.health.health_system.controller;

import com.health.health_system.entity.HealthRecord;
import com.health.health_system.entity.User;
import com.health.health_system.repository.HealthAnalysisRepository;
import com.health.health_system.repository.UserRepository;
import com.health.health_system.service.HealthAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/health-analysis")
@CrossOrigin(origins = "*")
public class HealthAnalysisController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthAnalysisRepository healthAnalysisRepository;

    @Autowired
    private HealthAnalysisService healthAnalysisService;

    // ==================== 新增：健康状态接口 ====================

    // 获取用户当前健康状态
    @GetMapping("/status/{userId}")
    public Map<String, Object> getCurrentHealthStatus(@PathVariable Integer userId) {
        return healthAnalysisService.getCurrentHealthStatus(userId);
    }

    // 获取趋势数据（用于图表）
    @GetMapping("/trend/{userId}")
    public Map<String, Object> getTrendData(
            @PathVariable Integer userId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return healthAnalysisService.getTrendData(userId, period, startDate, endDate);
    }

    // 获取自定义时间段报告
    @GetMapping("/custom-report/{userId}")
    public Map<String, Object> getCustomReport(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return healthAnalysisService.getCustomReport(userId, startDate, endDate);
    }

    // 获取健康小贴士
    @GetMapping("/tips/{userId}")
    public List<String> getHealthTips(@PathVariable Integer userId) {
        return healthAnalysisService.getHealthTips(userId);
    }

    // ==================== 原有：智能推荐接口 ====================

    // 获取个性化推荐
    @GetMapping("/recommend/{userId}")
    public Map<String, Object> getRecommendations(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();

        double bmi = calculateBMI(user);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        List<HealthRecord> recentRecords = healthAnalysisRepository
                .findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        double avgCaloriesBurned = calculateAvgCaloriesBurned(recentRecords);

        int consecutiveDays = getConsecutiveDays(userId);

        result.put("success", true);
        result.put("bmi", bmi);
        result.put("bmiStatus", getBMIStatus(bmi));
        result.put("sportRecommendations", getSportRecommendations(bmi, avgCaloriesBurned));
        result.put("dietRecommendations", getDietRecommendations(bmi, user.getBodyFat()));
        result.put("motivationMessage", getMotivationMessage(consecutiveDays, bmi));
        result.put("healthTips", getHealthTips(bmi));

        return result;
    }

    // ==================== 原有：健康报告接口 ====================

    @GetMapping("/report/{userId}")
    public Map<String, Object> getHealthReport(
            @PathVariable Integer userId,
            @RequestParam String period) {

        Map<String, Object> result = new HashMap<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDateObj;

        if ("week".equals(period)) {
            startDateObj = endDate.minusDays(7);
        } else {
            startDateObj = endDate.minusDays(30);
        }

        return generateReport(userId, startDateObj, endDate, period);
    }

    @GetMapping("/report/range/{userId}")
    public Map<String, Object> getReportByDateRange(
            @PathVariable Integer userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        return generateReport(userId, start, end, "custom");
    }

    // ==================== 原有：健康数据接口 ====================

    @GetMapping("/history/{userId}")
    public List<HealthRecord> getHistory(@PathVariable Integer userId) {
        return healthAnalysisRepository.findByUserIdOrderByRecordDateAsc(userId);
    }

    @GetMapping("/weight-history/{userId}")
    public Map<String, Object> getWeightHistory(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        List<HealthRecord> records = healthAnalysisRepository.findByUserIdOrderByRecordDateAsc(userId);

        List<String> dates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        for (HealthRecord record : records) {
            if (record.getWeight() != null) {
                dates.add(record.getRecordDate().toString());
                weights.add(record.getWeight());
            }
        }

        result.put("dates", dates);
        result.put("weights", weights);
        return result;
    }

    // ==================== 修复：all-history接口，确保数组长度一致 ====================
    @GetMapping("/all-history/{userId}")
    public Map<String, Object> getAllHistory(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        List<HealthRecord> records = healthAnalysisRepository.findByUserIdOrderByRecordDateAsc(userId);

        List<String> dates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        List<Double> bodyFats = new ArrayList<>();
        List<Double> chests = new ArrayList<>();
        List<Double> waists = new ArrayList<>();
        List<Double> hips = new ArrayList<>();
        List<Double> thighs = new ArrayList<>();
        List<Integer> heartRates = new ArrayList<>();
        List<Double> sleepDurations = new ArrayList<>();

        for (HealthRecord record : records) {
            dates.add(record.getRecordDate().toString());
            // 重要：每条记录都要添加值，没有值就加 null，保持数组长度一致
            weights.add(record.getWeight());
            bodyFats.add(record.getBodyFat());
            chests.add(record.getChest());
            waists.add(record.getWaist());
            hips.add(record.getHip());
            thighs.add(record.getThigh());
            heartRates.add(record.getRestingHeartRate());
            sleepDurations.add(record.getSleepDuration());
        }

        result.put("dates", dates);
        result.put("weights", weights);
        result.put("bodyFats", bodyFats);
        result.put("chests", chests);
        result.put("waists", waists);
        result.put("hips", hips);
        result.put("thighs", thighs);
        result.put("heartRates", heartRates);
        result.put("sleepDurations", sleepDurations);

        return result;
    }

    @PostMapping("/record")
    public Map<String, Object> recordHealth(
            @RequestParam Integer userId,
            @RequestParam(required = false) Double height,
            @RequestParam(required = false) Double weight,
            @RequestParam(required = false) Double bodyFat,
            @RequestParam(required = false) Double chest,
            @RequestParam(required = false) Double waist,
            @RequestParam(required = false) Double hip,
            @RequestParam(required = false) Double thigh,
            @RequestParam(required = false) Integer restingHeartRate,
            @RequestParam(required = false) Double sleepDuration,
            @RequestParam(required = false) Integer waterIntake,
            @RequestParam(required = false) String recordDate) {

        Map<String, Object> result = new HashMap<>();

        LocalDate date = (recordDate != null) ? LocalDate.parse(recordDate) : LocalDate.now();

        HealthRecord record = healthAnalysisService.addHealthRecord(
                userId, height, weight, bodyFat, chest, waist, hip, thigh,
                restingHeartRate, sleepDuration, waterIntake, date);

        result.put("success", true);
        result.put("message", "记录成功");
        result.put("bmi", record.getBmi());
        result.put("record", record);
        return result;
    }

    // ==================== 通用报告生成方法 ====================

    private Map<String, Object> generateReport(Integer userId, LocalDate startDate, LocalDate endDate, String period) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        User user = userOpt.get();

        List<HealthRecord> records = healthAnalysisRepository
                .findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        double startWeight = 0, endWeight = 0;
        double startBodyFat = 0, endBodyFat = 0;
        double avgSleep = 0, avgHeartRate = 0;
        int totalWater = 0;
        double maxWeight = 0, minWeight = 0;

        if (!records.isEmpty()) {
            HealthRecord first = records.get(0);
            HealthRecord last = records.get(records.size() - 1);
            startWeight = first.getWeight() != null ? first.getWeight() : 0;
            endWeight = last.getWeight() != null ? last.getWeight() : 0;
            startBodyFat = first.getBodyFat() != null ? first.getBodyFat() : 0;
            endBodyFat = last.getBodyFat() != null ? last.getBodyFat() : 0;

            double sumSleep = 0, sumHeartRate = 0;
            int sleepCount = 0, heartRateCount = 0;

            for (HealthRecord record : records) {
                if (record.getWeight() != null) {
                    if (maxWeight == 0 || record.getWeight() > maxWeight) maxWeight = record.getWeight();
                    if (minWeight == 0 || record.getWeight() < minWeight) minWeight = record.getWeight();
                }
                if (record.getSleepDuration() != null) {
                    sumSleep += record.getSleepDuration();
                    sleepCount++;
                }
                if (record.getRestingHeartRate() != null) {
                    sumHeartRate += record.getRestingHeartRate();
                    heartRateCount++;
                }
                if (record.getWaterIntake() != null) totalWater += record.getWaterIntake();
            }

            avgSleep = sleepCount > 0 ? Math.round(sumSleep / sleepCount * 10) / 10.0 : 0;
            avgHeartRate = heartRateCount > 0 ? Math.round(sumHeartRate / heartRateCount) : 0;
        } else {
            if (user.getCurrentWeight() != null) {
                startWeight = user.getCurrentWeight();
                endWeight = user.getCurrentWeight();
            }
        }

        double weightChange = endWeight - startWeight;
        double bodyFatChange = endBodyFat - startBodyFat;

        String advice = generateHealthAdvice(weightChange, bodyFatChange, avgSleep, avgHeartRate, totalWater);

        result.put("success", true);
        result.put("period", period);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalDays", records.size());
        result.put("startWeight", startWeight);
        result.put("endWeight", endWeight);
        result.put("weightChange", Math.round(weightChange * 10) / 10.0);
        result.put("weightChangeRate", startWeight > 0 ? Math.round((weightChange / startWeight) * 1000) / 10.0 : 0);
        result.put("startBodyFat", startBodyFat);
        result.put("endBodyFat", endBodyFat);
        result.put("bodyFatChange", Math.round(bodyFatChange * 10) / 10.0);
        result.put("maxWeight", maxWeight);
        result.put("minWeight", minWeight);
        result.put("avgSleep", avgSleep);
        result.put("avgHeartRate", avgHeartRate);
        result.put("totalWater", totalWater);
        result.put("advice", advice);

        return result;
    }

    // ==================== 辅助方法 ====================

    private double calculateBMI(User user) {
        Double height = user.getCurrentHeight();
        Double weight = user.getCurrentWeight();
        if (height == null || weight == null || height <= 0) {
            return 0;
        }
        double heightInMeters = height / 100;
        return Math.round((weight / (heightInMeters * heightInMeters)) * 10) / 10.0;
    }

    private String getBMIStatus(double bmi) {
        if (bmi <= 0) return "数据不足";
        if (bmi < 18.5) return "偏瘦";
        if (bmi < 24) return "正常";
        if (bmi < 28) return "偏重";
        return "肥胖";
    }

    private double calculateAvgCaloriesBurned(List<HealthRecord> records) {
        return 0;
    }

    private int getConsecutiveDays(Integer userId) {
        return 3;
    }

    private List<Map<String, String>> getSportRecommendations(double bmi, double avgCalories) {
        List<Map<String, String>> recommendations = new ArrayList<>();

        if (bmi >= 24) {
            recommendations.add(createRecommendation("🏃 有氧运动", "慢跑、快走、游泳等有氧运动，每周3-4次，每次30分钟以上"));
            recommendations.add(createRecommendation("🔥 燃脂训练", "HIIT高强度间歇训练，燃脂效率高，每次15-20分钟"));
        } else if (bmi < 18.5) {
            recommendations.add(createRecommendation("💪 力量训练", "深蹲、俯卧撑、引体向上等，增加肌肉量"));
            recommendations.add(createRecommendation("🍗 增肌饮食", "配合高蛋白饮食，运动后30分钟内补充蛋白质"));
        } else {
            recommendations.add(createRecommendation("🏃 维持训练", "每周3-5次中等强度运动，保持当前状态"));
            recommendations.add(createRecommendation("🧘 交叉训练", "结合有氧和力量训练，全面发展身体素质"));
        }

        return recommendations;
    }

    private List<Map<String, String>> getDietRecommendations(double bmi, Double bodyFat) {
        List<Map<String, String>> recommendations = new ArrayList<>();

        if (bmi >= 24 || (bodyFat != null && bodyFat > 25)) {
            recommendations.add(createRecommendation("🥗 低脂饮食", "增加蔬菜摄入，减少油炸食品和甜食"));
            recommendations.add(createRecommendation("💧 充足饮水", "每天饮水2000ml以上，促进新陈代谢"));
        } else if (bmi < 18.5) {
            recommendations.add(createRecommendation("🍚 增加碳水", "适量增加米饭、面食等优质碳水"));
            recommendations.add(createRecommendation("🥩 优质蛋白", "鸡胸肉、鱼肉、鸡蛋、豆制品"));
        } else {
            recommendations.add(createRecommendation("⚖️ 均衡饮食", "主食、蛋白质、蔬菜比例3:3:4"));
            recommendations.add(createRecommendation("🍎 多样搭配", "不同颜色的蔬菜水果，营养更全面"));
        }

        return recommendations;
    }

    private String getMotivationMessage(int consecutiveDays, double bmi) {
        if (consecutiveDays >= 30) {
            return "🎉 太棒了！你已经连续打卡30天，自律王者就是你！";
        } else if (consecutiveDays >= 14) {
            return "🌟 连续打卡14天！习惯正在养成，继续加油！";
        } else if (consecutiveDays >= 7) {
            return "✨ 连续打卡一周！你已经超越了大多数人！";
        } else if (consecutiveDays >= 3) {
            return "💪 连续打卡3天！好的开始是成功的一半！";
        } else if (consecutiveDays > 0) {
            return "🌱 你已经迈出了第一步，继续坚持！";
        } else {
            return "🚀 今天就开始行动吧！从一个小目标开始！";
        }
    }

    private List<String> getHealthTips(double bmi) {
        List<String> tips = new ArrayList<>();
        tips.add("🏃 运动后记得拉伸，帮助肌肉恢复");
        tips.add("💧 早起一杯温水，唤醒身体");
        tips.add("😴 保证7-8小时睡眠，身体修复的关键");

        if (bmi >= 24) {
            tips.add("📉 减重不是越快越好，每周0.5-1kg最健康");
        } else if (bmi < 18.5) {
            tips.add("🍚 少量多餐，更容易吸收营养");
        }

        return tips;
    }

    private String generateHealthAdvice(double weightChange, double bodyFatChange,
                                        double avgSleep, double avgHeartRate, int totalWater) {
        StringBuilder advice = new StringBuilder();

        if (weightChange < -0.5) {
            advice.append("✅ 体重控制得很好，继续坚持！");
        } else if (weightChange > 0.5) {
            advice.append("⚠️ 体重有所增加，建议增加运动量，控制饮食。");
        } else {
            advice.append("✅ 体重保持稳定，继续保持良好习惯。");
        }

        if (bodyFatChange < -0.5) {
            advice.append(" 体脂率下降明显，减脂效果显著！");
        } else if (bodyFatChange > 0.5) {
            advice.append(" 体脂率有所上升，建议增加有氧运动。");
        }

        if (avgSleep < 7 && avgSleep > 0) {
            advice.append(" 睡眠时长不足7小时，建议早睡早起。");
        } else if (avgSleep >= 8) {
            advice.append(" 睡眠充足，继续保持！");
        }

        if (avgHeartRate > 90 && avgHeartRate > 0) {
            advice.append(" 静息心率偏高，注意放松心情。");
        } else if (avgHeartRate < 60 && avgHeartRate > 0) {
            advice.append(" 心率偏低，如有不适请咨询医生。");
        }

        if (totalWater < 7000 && totalWater > 0) {
            advice.append(" 饮水量不足，建议每天喝够2000ml水。");
        }

        return advice.toString();
    }

    private Map<String, String> createRecommendation(String title, String description) {
        Map<String, String> rec = new HashMap<>();
        rec.put("title", title);
        rec.put("description", description);
        return rec;
    }

    // ==================== 新增：热量平衡分析 ====================

    @GetMapping("/calorie-balance/{userId}")
    public Map<String, Object> getCalorieBalance(@PathVariable Integer userId) {
        return healthAnalysisService.getCalorieBalance(userId);
    }

    @GetMapping("/sport-analysis/{userId}")
    public Map<String, Object> getSportAnalysis(@PathVariable Integer userId) {
        return healthAnalysisService.getSportAnalysis(userId);
    }

    @GetMapping("/nutrition-analysis/{userId}")
    public Map<String, Object> getNutritionAnalysis(@PathVariable Integer userId) {
        return healthAnalysisService.getNutritionAnalysis(userId);
    }

    @GetMapping("/habit-analysis/{userId}")
    public Map<String, Object> getHabitAnalysis(@PathVariable Integer userId) {
        return healthAnalysisService.getHabitAnalysis(userId);
    }

    @GetMapping("/insights/{userId}")
    public List<String> getSmartInsights(@PathVariable Integer userId) {
        return healthAnalysisService.getSmartInsights(userId);
    }

    @GetMapping("/correlation-trend/{userId}")
    public Map<String, Object> getCorrelationTrend(@PathVariable Integer userId) {
        return healthAnalysisService.getCorrelationTrend(userId);
    }

    // ==================== 新增：健康综合评分 ====================

    @GetMapping("/health-score/{userId}")
    public Map<String, Object> getHealthScore(@PathVariable Integer userId) {
        return healthAnalysisService.getHealthScore(userId);
    }
}