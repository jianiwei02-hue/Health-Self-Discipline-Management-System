package com.health.health_system.service;

import com.health.health_system.entity.ItemLibrary;
import com.health.health_system.entity.SportRecord;
import com.health.health_system.entity.User;
import com.health.health_system.repository.ItemLibraryRepository;
import com.health.health_system.repository.SportRecordRepository;
import com.health.health_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SportRecordService {

    @Autowired
    private SportRecordRepository sportRecordRepository;

    @Autowired
    private ItemLibraryRepository itemLibraryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 根据运动名称获取 MET 值
     */
    private double getMetValue(String sportName) {
        Optional<ItemLibrary> sportOpt = itemLibraryRepository.findByTypeAndName("SPORT", sportName);
        if (sportOpt.isPresent() && sportOpt.get().getMetValue() != null) {
            return sportOpt.get().getMetValue();
        }

        List<ItemLibrary> sports = itemLibraryRepository.findByTypeAndNameContaining("SPORT", sportName);
        for (ItemLibrary sport : sports) {
            if (sport.getMetValue() != null) {
                return sport.getMetValue();
            }
        }

        return getMetByKeyword(sportName);
    }

    private double getMetByKeyword(String sportName) {
        if (sportName.contains("跑步") || sportName.contains("run")) return 8.0;
        if (sportName.contains("慢跑") || sportName.contains("jog")) return 7.0;
        if (sportName.contains("快跑") || sportName.contains("sprint")) return 10.0;
        if (sportName.contains("散步") || sportName.contains("walk")) return 3.5;
        if (sportName.contains("快走")) return 5.0;
        if (sportName.contains("跳绳") || sportName.contains("skip")) return 10.0;
        if (sportName.contains("游泳") || sportName.contains("swim")) return 7.0;
        if (sportName.contains("骑行") || sportName.contains("bike")) return 6.0;
        if (sportName.contains("瑜伽") || sportName.contains("yoga")) return 3.0;
        if (sportName.contains("深蹲") || sportName.contains("squat")) return 5.0;
        if (sportName.contains("俯卧撑") || sportName.contains("pushup")) return 4.0;
        if (sportName.contains("引体向上") || sportName.contains("pullup")) return 5.0;
        if (sportName.contains("篮球") || sportName.contains("basketball")) return 7.0;
        if (sportName.contains("足球") || sportName.contains("football")) return 8.0;
        if (sportName.contains("羽毛球") || sportName.contains("badminton")) return 6.0;
        if (sportName.contains("帕梅拉")) return 5.0;
        if (sportName.contains("拉伸") || sportName.contains("stretch")) return 4.5;
        return 5.0;
    }

    private double getUserWeight(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getCurrentWeight() != null) {
            return userOpt.get().getCurrentWeight();
        }
        return 65.0;
    }

    private int calculateCalories(String sportName, Integer duration, Integer userId) {
        if (duration == null || duration <= 0) return 0;
        double met = getMetValue(sportName);
        double weight = getUserWeight(userId);
        double calories = met * weight * 0.0175 * duration;
        return (int) Math.round(calories);
    }

    /**
     * 添加运动记录（自动计算热量 + 自动更新活动量）
     */
    public SportRecord addSportRecord(Integer userId, String sportName,
                                      Integer duration, Integer caloriesBurned,
                                      LocalDate recordDate) {
        int finalCalories;
        if (caloriesBurned != null && caloriesBurned > 0) {
            finalCalories = caloriesBurned;
        } else {
            finalCalories = calculateCalories(sportName, duration, userId);
        }

        SportRecord record = new SportRecord();
        record.setUserId(userId);
        record.setSportName(sportName);
        record.setDuration(duration != null ? duration : 0);
        record.setCaloriesBurned(finalCalories);
        record.setRecordDate(recordDate != null ? recordDate : LocalDate.now());
        record.setCreateTime(LocalDateTime.now());

        SportRecord saved = sportRecordRepository.save(record);

        // 自动更新用户的活动量
        updateUserActivityLevel(userId);

        return saved;
    }

    /**
     * 更新运动记录（自动重新计算热量 + 自动更新活动量）
     */
    public SportRecord updateSportRecord(Integer recordId, Integer userId, String sportName,
                                         Integer duration, Integer caloriesBurned) {
        SportRecord record = sportRecordRepository.findById(recordId).orElse(null);
        if (record == null || !record.getUserId().equals(userId)) {
            return null;
        }

        record.setSportName(sportName);
        record.setDuration(duration != null ? duration : 0);

        if (caloriesBurned != null && caloriesBurned > 0) {
            record.setCaloriesBurned(caloriesBurned);
        } else {
            record.setCaloriesBurned(calculateCalories(sportName, duration, userId));
        }

        SportRecord saved = sportRecordRepository.save(record);

        // 自动更新用户的活动量
        updateUserActivityLevel(userId);

        return saved;
    }

    /**
     * 删除运动记录（同时更新活动量）
     */
    public boolean deleteRecord(Integer recordId, Integer userId) {
        SportRecord record = sportRecordRepository.findById(recordId).orElse(null);
        if (record != null && record.getUserId().equals(userId)) {
            sportRecordRepository.deleteById(recordId);
            // 删除后重新计算活动量
            updateUserActivityLevel(userId);
            return true;
        }
        return false;
    }

    /**
     * 根据用户最近7天的运动数据自动计算活动量
     */
    private void updateUserActivityLevel(Integer userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 最近7天

        List<SportRecord> records = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        // 统计最近7天的运动次数和总消耗
        int workoutCount = records.size();
        int totalCalories = records.stream().mapToInt(SportRecord::getCaloriesBurned).sum();

        String activityLevel;

        if (workoutCount == 0) {
            activityLevel = "SEDENTARY";      // 久坐：没有运动
        } else if (workoutCount <= 2 && totalCalories < 500) {
            activityLevel = "LIGHT";           // 轻度：1-2次运动，消耗<500
        } else if (workoutCount >= 3 && workoutCount <= 5 && totalCalories >= 500 && totalCalories <= 1500) {
            activityLevel = "MODERATE";        // 中度：3-5次，消耗500-1500
        } else if (workoutCount >= 6 && totalCalories >= 1500 && totalCalories <= 3000) {
            activityLevel = "ACTIVE";          // 活跃：6-7次，消耗1500-3000
        } else if (workoutCount >= 7 || totalCalories > 3000) {
            activityLevel = "VERY_ACTIVE";     // 高强度：7次以上或消耗>3000
        } else {
            activityLevel = "MODERATE";        // 默认中度
        }

        // 更新用户的活动量
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!activityLevel.equals(user.getActivityLevel())) {
                user.setActivityLevel(activityLevel);
                userRepository.save(user);
                System.out.println("用户 " + userId + " 活动量自动更新为: " + activityLevel +
                        " (运动次数: " + workoutCount + ", 总消耗: " + totalCalories + "大卡)");
            }
        }
    }

    // ========== 原有方法保持不变 ==========

    public List<SportRecord> getDailySport(Integer userId, LocalDate recordDate) {
        return sportRecordRepository.findByUserIdAndRecordDateOrderByIdDesc(userId, recordDate);
    }

    public List<SportRecord> getAllSportRecords(Integer userId) {
        return sportRecordRepository.findByUserIdOrderByRecordDateDesc(userId);
    }

    public List<SportRecord> getUserSportRecords(Integer userId) {
        return sportRecordRepository.findByUserIdOrderByRecordDateDesc(userId);
    }

    public Map<String, Object> getDailySummary(Integer userId, LocalDate recordDate) {
        Map<String, Object> result = new HashMap<>();
        List<SportRecord> records = getDailySport(userId, recordDate);
        Integer totalCalories = sportRecordRepository.sumCaloriesByUserIdAndRecordDate(userId, recordDate);
        result.put("records", records);
        result.put("totalCaloriesBurned", totalCalories != null ? totalCalories : 0);
        result.put("recordDate", recordDate);
        return result;
    }

    public Map<String, Object> getSportStats(Integer userId, String period) {
        Map<String, Object> result = new HashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = "week".equals(period) ? endDate.minusDays(6) :
                ("month".equals(period) ? endDate.minusDays(29) : endDate.minusDays(6));

        List<SportRecord> records = sportRecordRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        Map<LocalDate, Integer> dailyStats = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> caloriesList = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dailyStats.put(current, 0);
            dates.add(current.toString());
            current = current.plusDays(1);
        }

        for (SportRecord record : records) {
            LocalDate date = record.getRecordDate();
            if (dailyStats.containsKey(date)) {
                dailyStats.put(date, dailyStats.get(date) + record.getCaloriesBurned());
            }
        }

        for (LocalDate date : dailyStats.keySet()) {
            caloriesList.add(dailyStats.get(date));
        }

        result.put("period", period);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("dates", dates);
        result.put("calories", caloriesList);

        int totalCalories = caloriesList.stream().mapToInt(Integer::intValue).sum();
        double avgCalories = caloriesList.stream().mapToInt(Integer::intValue).average().orElse(0);

        result.put("totalCalories", totalCalories);
        result.put("avgCalories", Math.round(avgCalories));
        return result;
    }
}