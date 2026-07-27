package com.health.health_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaiduAIConfig {

    @Value("${baidu.ai.app.id}")
    private String appId;

    @Value("${baidu.ai.api.key}")
    private String apiKey;

    @Value("${baidu.ai.secret.key}")
    private String secretKey;

    public String getAppId() {
        return appId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
}