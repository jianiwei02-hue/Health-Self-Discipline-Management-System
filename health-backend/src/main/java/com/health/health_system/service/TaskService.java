package com.health.health_system.service;

import com.health.health_system.entity.CheckInSummary;
import com.health.health_system.entity.HabitTask;
import com.health.health_system.repository.CheckInSummaryRepository;
import com.health.health_system.repository.HabitTaskRepository;
import com.health.health_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TaskService {

    @Autowired
    private MedalService medalService;

    @Autowired
    private HabitTaskRepository habitTaskRepository;

    @Autowired
    private CheckInSummaryRepository checkInSummaryRepository;

    @Autowired
    private SportRecordService sportRecordService;

    @Autowired
    private DietRecordService dietRecordService;

    @Autowired
    private UserRepository userRepository;

    // 创建任务
    public HabitTask createTask(Integer userId, String taskName, String taskType,
                                String targetValue, LocalDate recordDate) {
        HabitTask task = new HabitTask();
        task.setUserId(userId);
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setTargetValue(targetValue);
        task.setRecordDate(recordDate);
        task.setStatus(0);
        return habitTaskRepository.save(task);
    }

    // 修改任务（仅允许修改未完成的任务）
    public HabitTask updateTask(Integer taskId, String taskName, String taskType) {
        HabitTask task = habitTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() == 1) {
            return null;
        }
        if (taskName != null && !taskName.trim().isEmpty()) {
            task.setTaskName(taskName);
        }
        if (taskType != null && !taskType.trim().isEmpty()) {
            task.setTaskType(taskType);
        }
        return habitTaskRepository.save(task);
    }

    // 完成任务（打卡）
    public Map<String, Object> completeTask(Integer taskId) {
        Map<String, Object> result = new HashMap<>();

        HabitTask task = habitTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            result.put("success", false);
            result.put("message", "任务不存在");
            return result;
        }

        if (task.getStatus() == 1) {
            result.put("success", false);
            result.put("message", "任务已完成过了");
            return result;
        }

        // 根据任务类型同步到对应记录表
        try {
            String taskType = task.getTaskType();
            String taskName = task.getTaskName();
            Integer userId = task.getUserId();
            LocalDate today = LocalDate.now();

            if ("SPORT".equalsIgnoreCase(taskType)) {
                syncToSportRecord(userId, taskName, task.getTargetValue(), today);
            } else if ("DIET".equalsIgnoreCase(taskType)) {
                syncToDietRecord(userId, taskName, task.getTargetValue(), today);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        task.setStatus(1);
        habitTaskRepository.save(task);

        updateCheckInSummary(task.getUserId());
        medalService.checkAndAwardMedals(task.getUserId());

        result.put("success", true);
        result.put("message", "打卡成功！");
        return result;
    }

    /**
     * 同步运动任务到运动记录表
     */
    private void syncToSportRecord(Integer userId, String taskName, String targetValue, LocalDate recordDate) {
        try {
            String sportName = taskName;
            Integer durationMinutes = null;
            Double distance = null;
            String processedName = taskName;

            Pattern distancePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(km|公里|千米|m|米|KM)", Pattern.CASE_INSENSITIVE);
            Matcher distanceMatcher = distancePattern.matcher(processedName);
            if (distanceMatcher.find()) {
                double value = Double.parseDouble(distanceMatcher.group(1));
                String unit = distanceMatcher.group(2).toLowerCase();
                if ("km".equals(unit) || "公里".equals(unit) || "千米".equals(unit)) {
                    distance = value;
                } else if ("m".equals(unit) || "米".equals(unit)) {
                    distance = value / 1000;
                }
                processedName = processedName.replaceAll("\\s*\\d+(?:\\.\\d+)?\\s*(km|公里|千米|m|米)", "").trim();
            }

            Pattern timePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(分钟|min|分|小时|h)");
            Matcher timeMatcher = timePattern.matcher(processedName);
            if (timeMatcher.find()) {
                double value = Double.parseDouble(timeMatcher.group(1));
                String unit = timeMatcher.group(2);
                if (unit.contains("小时") || unit.equals("h")) {
                    durationMinutes = (int) Math.round(value * 60);
                } else {
                    durationMinutes = (int) Math.round(value);
                }
                processedName = processedName.replaceAll("\\s*\\d+(?:\\.\\d+)?\\s*(分钟|min|分|小时|h)", "").trim();
            } else {
                if (processedName.contains("半小时")) {
                    durationMinutes = 30;
                    processedName = processedName.replace("半小时", "").trim();
                } else if (processedName.contains("一小时") || processedName.contains("1小时")) {
                    durationMinutes = 60;
                    processedName = processedName.replaceAll("一小时|1小时", "").trim();
                } else if (processedName.contains("一个半小时")) {
                    durationMinutes = 90;
                    processedName = processedName.replace("一个半小时", "").trim();
                } else if (processedName.contains("两小时") || processedName.contains("2小时")) {
                    durationMinutes = 120;
                    processedName = processedName.replaceAll("两小时|2小时", "").trim();
                }
            }

            sportName = processedName;
            if (sportName == null || sportName.isEmpty()) {
                if (taskName.contains("跑")) sportName = "跑步";
                else if (taskName.contains("走")) sportName = "散步";
                else if (taskName.contains("游")) sportName = "游泳";
                else if (taskName.contains("骑")) sportName = "骑行";
                else if (taskName.contains("跳")) sportName = "跳绳";
                else if (taskName.contains("瑜伽")) sportName = "瑜伽";
                else sportName = "运动";
            }

            if (distance != null && distance > 0 && (durationMinutes == null || durationMinutes <= 0)) {
                durationMinutes = (int) Math.round(distance * 6);
                if (durationMinutes < 5) durationMinutes = 5;
            }

            if (durationMinutes == null || durationMinutes <= 0) {
                durationMinutes = 30;
            }
            if (durationMinutes > 480) {
                durationMinutes = 480;
            }

            sportRecordService.addSportRecord(userId, sportName, durationMinutes, null, recordDate);

            System.out.println("同步运动任务成功: " + sportName + ", " + durationMinutes + "分钟" +
                    (distance != null ? " (距离: " + distance + "km)" : ""));
        } catch (Exception e) {
            System.err.println("同步运动任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 同步饮食任务到饮食记录表
     */
    private void syncToDietRecord(Integer userId, String taskName, String targetValue, LocalDate recordDate) {
        try {
            String foodName = taskName;
            Integer quantity = null;

            Pattern weightPattern = Pattern.compile("(\\d+)\\s*(g|克|ml|毫升)");
            Matcher weightMatcher = weightPattern.matcher(taskName);
            if (weightMatcher.find()) {
                quantity = Integer.parseInt(weightMatcher.group(1));
                foodName = taskName.replaceAll("\\s*\\d+\\s*(g|克|ml|毫升)", "").trim();
            }

            if (quantity == null || quantity <= 0) {
                quantity = 100;
            }
            if (quantity > 5000) {
                quantity = 500;
            }
            if (foodName == null || foodName.isEmpty()) {
                foodName = taskName;
            }

            int calories = (int) Math.round(0.5 * quantity);
            if (calories < 10) calories = 50;

            dietRecordService.addDietRecord(userId, "SNACK", foodName, quantity, calories, recordDate,
                    null, null, null, null, null, null, null);

            System.out.println("同步饮食任务成功: " + foodName + ", " + quantity + "g, " + calories + "大卡");
        } catch (Exception e) {
            System.err.println("同步饮食任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Double getUserWeight(Integer userId) {
        return userRepository.findById(userId)
                .map(user -> user.getCurrentWeight())
                .orElse(65.0);
    }

    /**
     * 更新打卡汇总（修复版）
     * 只要当天有任意任务完成就算打卡，计算连续天数
     */
    private void updateCheckInSummary(Integer userId) {
        LocalDate today = LocalDate.now();

        // 获取或创建汇总记录
        CheckInSummary summary = checkInSummaryRepository.findByUserId(userId).orElse(null);
        if (summary == null) {
            summary = new CheckInSummary();
            summary.setUserId(userId);
            summary.setTotalDays(0);
            summary.setConsecutiveDays(0);
            summary.setLastCheckInDate(null);
        }

        // 检查今天是否有任务完成
        List<HabitTask> todayCompletedTasks = habitTaskRepository.findByUserIdAndRecordDateAndStatus(userId, today, 1);
        boolean hasCompletedToday = !todayCompletedTasks.isEmpty();

        LocalDate lastDate = summary.getLastCheckInDate();
        int consecutive = summary.getConsecutiveDays();
        int total = summary.getTotalDays();

        if (hasCompletedToday) {
            LocalDate expectedLastDate = today.minusDays(1);

            if (lastDate != null && lastDate.equals(expectedLastDate)) {
                // 昨天也有打卡，连续天数+1
                consecutive++;
            } else if (lastDate != null && lastDate.equals(today)) {
                // 今天已经记录过，不重复增加
                checkInSummaryRepository.save(summary);
                return;
            } else {
                // 断签或首次打卡，从1开始
                consecutive = 1;
            }

            // 更新总打卡天数（只在第一次打卡当天时增加）
            if (lastDate == null || !lastDate.equals(today)) {
                total++;
            }

            summary.setLastCheckInDate(today);
            summary.setConsecutiveDays(consecutive);
            summary.setTotalDays(total);
        } else {
            // 今天没有完成任务，重置连续天数（但保留总天数）
            if (lastDate != null && !lastDate.equals(today)) {
                summary.setConsecutiveDays(0);
            }
        }

        checkInSummaryRepository.save(summary);
        System.out.println("用户 " + userId + " 打卡汇总: 总天数=" + summary.getTotalDays() +
                ", 连续天数=" + summary.getConsecutiveDays() + ", 最后打卡=" + summary.getLastCheckInDate());
    }

    // 获取用户今日任务列表
    public List<HabitTask> getTodayTasks(Integer userId) {
        return habitTaskRepository.findByUserIdAndRecordDate(userId, LocalDate.now());
    }

    // 获取用户打卡汇总信息
    public CheckInSummary getCheckInSummary(Integer userId) {
        return checkInSummaryRepository.findByUserId(userId).orElse(null);
    }

    // 获取用户连续打卡天数
    public int getConsecutiveDays(Integer userId) {
        CheckInSummary summary = checkInSummaryRepository.findByUserId(userId).orElse(null);
        if (summary == null) {
            return 0;
        }
        return summary.getConsecutiveDays();
    }
}