package com.health.health_system.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // 加密密码
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 验证密码
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}