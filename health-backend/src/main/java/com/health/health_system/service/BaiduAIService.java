package com.health.health_system.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.health.health_system.config.BaiduAIConfig;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Base64;

@Service
public class BaiduAIService {

    @Autowired
    private BaiduAIConfig baiduAIConfig;

    private String accessToken = null;
    private long tokenExpireTime = 0;

    private final OkHttpClient client = new OkHttpClient();

    // 常见食物及其热量（大卡/100g）
    private static final String[][] COMMON_FOODS = {
            {"苹果", "52"}, {"香蕉", "89"}, {"橙子", "47"}, {"米饭", "116"},
            {"面条", "110"}, {"面包", "80"}, {"鸡蛋", "70"}, {"牛奶", "60"},
            {"鸡胸肉", "165"}, {"牛肉", "125"}, {"猪肉", "143"}, {"鱼", "105"},
            {"西兰花", "34"}, {"胡萝卜", "41"}, {"西红柿", "19"}, {"黄瓜", "15"},
            {"蛋糕", "348"}, {"饼干", "435"}, {"巧克力", "589"}, {"冰淇淋", "127"}
    };

    private String getAccessToken() throws IOException {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        String url = String.format(
                "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                baiduAIConfig.getApiKey(), baiduAIConfig.getSecretKey()
        );

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            System.out.println("Token获取响应: " + body);
            JSONObject json = JSONObject.parseObject(body);

            if (json.containsKey("error")) {
                String errorMsg = json.getString("error_description");
                System.err.println("获取Token失败: " + errorMsg);
                throw new IOException("百度Token获取失败: " + errorMsg);
            }

            accessToken = json.getString("access_token");
            int expiresIn = json.getIntValue("expires_in");
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;
            System.out.println("Token获取成功");
            return accessToken;
        }
    }

    public JSONObject recognizeDish(byte[] imageBytes) throws IOException {
        System.out.println("========== 开始调用百度API识别菜品 ==========");
        System.out.println("图片大小: " + imageBytes.length + " bytes");

        String accessToken = getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            String error = "无法获取百度access_token，请检查api.key和secret.key配置";
            System.err.println(error);
            throw new IOException(error);
        }

        System.out.println("AccessToken获取成功，长度: " + accessToken.length());

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String url = "https://aip.baidubce.com/rest/2.0/image-classify/v2/dish?access_token=" + accessToken;

        RequestBody body = new FormBody.Builder()
                .add("image", base64Image)
                .add("top_num", "5")
                .add("filter_threshold", "0.01")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String result = response.body().string();
            System.out.println("百度API原始返回: " + result);

            JSONObject jsonResult = JSONObject.parseObject(result);

            if (jsonResult.containsKey("error_code")) {
                int errorCode = jsonResult.getIntValue("error_code");
                String errorMsg = jsonResult.getString("error_msg");
                System.err.println("百度API错误: " + errorCode + " - " + errorMsg);
                throw new IOException("百度API错误: " + errorCode + " - " + errorMsg);
            }

            System.out.println("百度API调用成功");
            return jsonResult;
        } catch (Exception e) {
            System.err.println("调用百度API异常: " + e.getMessage());
            throw new IOException("调用百度API失败: " + e.getMessage(), e);
        }
    }

    public JSONObject parseDishResult(JSONObject result) {
        JSONObject parsed = new JSONObject();

        if (result == null) {
            System.out.println("result为空，返回默认结果");
            return getDefaultParseResult();
        }

        if (result.containsKey("error_code")) {
            System.out.println("百度API错误: " + result.getString("error_msg"));
            return getDefaultParseResult();
        }

        JSONArray results = result.getJSONArray("result");

        System.out.println("百度返回的result数组长度: " + (results == null ? "null" : results.size()));

        if (results != null && results.size() > 0) {
            JSONObject top = results.getJSONObject(0);
            String foodName = top.getString("name");
            double probability = top.getDoubleValue("probability");

            System.out.println("识别结果: " + foodName + ", 置信度: " + probability);

            // 只根据置信度判断，不再过滤不常见的名称
            if (probability < 0.2) {
                System.out.println("置信度过低(" + probability + ")，返回候选列表");
                return getDefaultParseResult();
            }

            // 获取热量（兼容字符串和对象两种格式）
            int defaultCalories = 100;
            if (top.containsKey("calorie")) {
                Object calorieObj = top.get("calorie");
                if (calorieObj != null) {
                    if (calorieObj instanceof Number) {
                        defaultCalories = ((Number) calorieObj).intValue();
                    } else if (calorieObj instanceof String) {
                        try {
                            defaultCalories = Integer.parseInt((String) calorieObj);
                        } catch (NumberFormatException e) {
                            defaultCalories = 100;
                        }
                    } else if (calorieObj instanceof JSONObject) {
                        JSONObject calorieJson = (JSONObject) calorieObj;
                        if (calorieJson.containsKey("calorie_value")) {
                            defaultCalories = calorieJson.getIntValue("calorie_value");
                        }
                    }
                }
            }

            JSONArray candidates = new JSONArray();
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                JSONObject candidate = new JSONObject();
                candidate.put("name", item.getString("name"));
                candidate.put("probability", item.getDoubleValue("probability"));

                // 获取热量（兼容字符串和对象两种格式）
                int calories = 100;
                if (item.containsKey("calorie")) {
                    Object calorieObj = item.get("calorie");
                    if (calorieObj != null) {
                        if (calorieObj instanceof Number) {
                            calories = ((Number) calorieObj).intValue();
                        } else if (calorieObj instanceof String) {
                            try {
                                calories = Integer.parseInt((String) calorieObj);
                            } catch (NumberFormatException e) {
                                calories = 100;
                            }
                        } else if (calorieObj instanceof JSONObject) {
                            JSONObject calorieJson = (JSONObject) calorieObj;
                            if (calorieJson.containsKey("calorie_value")) {
                                calories = calorieJson.getIntValue("calorie_value");
                            }
                        }
                    }
                }
                candidate.put("calories", calories);
                candidates.add(candidate);
            }

            parsed.put("success", true);
            parsed.put("foodName", foodName);
            parsed.put("probability", probability);
            parsed.put("estimatedCalories", defaultCalories);
            parsed.put("candidates", candidates);
            parsed.put("candidatesCount", candidates.size());
            parsed.put("message", candidates.size() > 1 ? "请选择正确的菜品" : "识别成功");

            System.out.println("返回给前端的数据: " + parsed.toJSONString());

        } else {
            System.out.println("未识别到菜品，返回默认结果");
            return getDefaultParseResult();
        }

        return parsed;
    }

    private JSONObject getDefaultParseResult() {
        JSONObject parsed = new JSONObject();
        JSONArray candidates = new JSONArray();

        for (String[] food : COMMON_FOODS) {
            JSONObject candidate = new JSONObject();
            candidate.put("name", food[0]);
            candidate.put("probability", 0.1);
            candidate.put("calories", Integer.parseInt(food[1]));
            candidates.add(candidate);
        }

        parsed.put("success", true);
        parsed.put("foodName", "请选择食物");
        parsed.put("probability", 0.5);
        parsed.put("estimatedCalories", 100);
        parsed.put("candidates", candidates);
        parsed.put("candidatesCount", candidates.size());
        parsed.put("message", "AI识别置信度低，请从下方选择食物");

        System.out.println("返回默认候选列表，数量: " + candidates.size());
        return parsed;
    }
}