package com.health.health_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HealthSystemApplication {

	public static void main(String[] args) {
		// 临时生成加密密码
		//org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
		//		new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
		//String encoded = encoder.encode("12345678");
		//System.out.println("========================================");
	//	System.out.println("12345678 加密后: " + encoded);
		// System.out.println("========================================");

		SpringApplication.run(HealthSystemApplication.class, args);
	}

	@GetMapping("/test")
	public String test() {
		return "项目启动成功！";
	}
}