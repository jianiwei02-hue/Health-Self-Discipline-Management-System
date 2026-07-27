package com.health.health_system.service;

import com.health.health_system.entity.CheckInSummary;
import com.health.health_system.entity.HabitTask;
import com.health.health_system.entity.MedalRule;
import com.health.health_system.entity.UserMedal;
import com.health.health_system.repository.CheckInSummaryRepository;
import com.health.health_system.repository.HabitTaskRepository;
import com.health.health_system.repository.MedalRuleRepository;
import com.health.health_system.repository.UserMedalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedalService {

    @Autowired
    private MedalRuleRepository medalRuleRepository;

    @Autowired
    private UserMedalRepository userMedalRepository;

    @Autowired
    private HabitTaskRepository habitTaskRepository;

    @Autowired
    private CheckInSummaryRepository checkInSummaryRepository;

    /**
     * 检查并颁发勋章（在打卡后调用）
     */
    public void checkAndAwardMedals(Integer userId) {
        // 1. 获取用户统计数据
        UserStats stats = getUserStats(userId);

        // 2. 获取所有勋章规则
        List<MedalRule> rules = medalRuleRepository.findAll();

        // 3. 遍历检查每个勋章
        for (MedalRule rule : rules) {
            // 检查用户是否已经获得该勋章
            if (userMedalRepository.existsByUserIdAndMedalId(userId, rule.getId())) {
                continue;
            }

            // 检查是否满足条件
            boolean satisfied = checkCondition(rule, stats, userId);

            if (satisfied) {
                UserMedal userMedal = new UserMedal();
                userMedal.setUserId(userId);
                userMedal.setMedalId(rule.getId());
                userMedal.setGetTime(LocalDateTime.now());
                userMedalRepository.save(userMedal);
                System.out.println("🎉 颁发勋章：" + rule.getMedalName() + " 给用户：" + userId);
            }
        }
    }

    /**
     * 获取用户的统计数据
     */
    private UserStats getUserStats(Integer userId) {
        UserStats stats = new UserStats();

        // 获取所有已完成的任务
        List<HabitTask> completedTasks = habitTaskRepository.findByUserIdAndStatus(userId, 1);

        // 总打卡次数
        stats.totalCount = completedTasks.size();

        // 按类型统计
        for (HabitTask task : completedTasks) {
            String type = task.getTaskType();
            if ("SPORT".equals(type)) {
                stats.sportCount++;
            } else if ("DIET".equals(type)) {
                stats.dietCount++;
            } else if ("STUDY".equals(type) || "OTHER".equals(type)) {
                stats.studyCount++;
            } else if ("SLEEP".equals(type)) {
                stats.sleepCount++;
            }
        }

        // 获取连续打卡天数
        CheckInSummary summary = checkInSummaryRepository.findByUserId(userId).orElse(null);
        if (summary != null) {
            stats.consecutiveDays = summary.getConsecutiveDays();
            stats.totalDays = summary.getTotalDays();
        }

        // 获取当月打卡天数
        stats.currentMonthDays = getCurrentMonthCheckInDays(userId);

        // 获取本周打卡天数
        stats.currentWeekDays = getCurrentWeekCheckInDays(userId);

        // 获取连续周完成情况
        stats.continuousWeeks = getContinuousWeeks(userId);

        // 获取早打卡次数（假设早上9点前打卡算早打卡）
        stats.earlyBirdCount = getEarlyBirdCount(userId);

        return stats;
    }

    /**
     * 获取当月打卡天数
     */
    private int getCurrentMonthCheckInDays(Integer userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        List<HabitTask> tasks = habitTaskRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);
        // 按日期去重计算打卡天数
        return (int) tasks.stream()
                .map(HabitTask::getRecordDate)
                .distinct()
                .count();
    }

    /**
     * 获取本周打卡天数
     */
    private int getCurrentWeekCheckInDays(Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<HabitTask> tasks = habitTaskRepository.findByUserIdAndRecordDateBetween(userId, startOfWeek, endOfWeek);
        return (int) tasks.stream()
                .map(HabitTask::getRecordDate)
                .distinct()
                .count();
    }

    /**
     * 获取连续几周完成目标（每周至少打卡5天）
     */
    private int getContinuousWeeks(Integer userId) {
        int consecutiveWeeks = 0;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 52; i++) { // 最多检查52周
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            long checkInDays = habitTaskRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd)
                    .stream()
                    .map(HabitTask::getRecordDate)
                    .distinct()
                    .count();

            if (checkInDays >= 5) {
                consecutiveWeeks++;
            } else {
                break;
            }
        }
        return consecutiveWeeks;
    }

    /**
     * 获取早打卡次数（打卡时间在早上9点之前）
     * 注意：需要修改 HabitTask 实体添加 createTime 字段
     */
    private int getEarlyBirdCount(Integer userId) {
        // 如果没有 createTime 字段，暂时返回 0
        // 如果需要此功能，需要在 HabitTask 实体中添加 createTime 字段
        // 并在打卡时记录时间
        return 0;
    }

    /**
     * 检查是否满足勋章条件
     */
    private boolean checkCondition(MedalRule rule, UserStats stats, Integer userId) {
        String type = rule.getConditionType();
        int required = rule.getConditionValue();

        switch (type) {
            case "total_count":
                return stats.totalCount >= required;
            case "consecutive_days":
                return stats.consecutiveDays >= required;
            case "sport_count":
                return stats.sportCount >= required;
            case "diet_count":
                return stats.dietCount >= required;
            case "study_count":
                return stats.studyCount >= required;
            case "sleep_count":
                return stats.sleepCount >= required;
            case "month_full":
                // 当月全勤：当月打卡天数 = 当月总天数
                YearMonth currentMonth = YearMonth.now();
                int totalDaysInMonth = currentMonth.lengthOfMonth();
                return stats.currentMonthDays >= totalDaysInMonth;
            case "EARLY_BIRD":
                return stats.earlyBirdCount >= required;
            case "READING":
                // 阅读文章次数需要从 health_article 表统计
                // 这里需要注入 HealthArticleRepository
                return getReadingCount(userId) >= required;
            case "TOTAL_DAYS":
                return stats.totalDays >= required;
            case "WEEKLY":
                return stats.continuousWeeks >= required;
            case "PERFECT_WEEK":
                // 完美一周：本周7天全部打卡
                return stats.currentWeekDays >= 7;
            default:
                return false;
        }
    }

    /**
     * 获取用户阅读文章次数
     */
    private int getReadingCount(Integer userId) {
        // 这里需要注入 HealthArticleRecordRepository
        // 如果没有该功能，暂时返回 0
        // 如果需要此功能，请添加对应的 Repository 并实现
        return 0;
    }

    /**
     * 获取用户的所有勋章
     */
    public List<UserMedal> getUserMedals(Integer userId) {
        return userMedalRepository.findByUserIdOrderByGetTimeDesc(userId);
    }

    /**
     * 获取所有勋章规则
     */
    public List<MedalRule> getAllMedalRules() {
        return medalRuleRepository.findAll();
    }

    /**
     * 内部类：用户统计数据
     */
    private static class UserStats {
        int totalCount = 0;          // 总打卡次数
        int consecutiveDays = 0;     // 连续打卡天数
        int totalDays = 0;           // 累计打卡天数
        int sportCount = 0;          // 运动任务完成次数
        int dietCount = 0;           // 饮食任务完成次数
        int studyCount = 0;          // 学习/其他任务完成次数
        int sleepCount = 0;          // 作息任务完成次数
        int currentMonthDays = 0;    // 当月打卡天数
        int currentWeekDays = 0;     // 本周打卡天数
        int continuousWeeks = 0;     // 连续周数
        int earlyBirdCount = 0;      // 早打卡次数
    }
}