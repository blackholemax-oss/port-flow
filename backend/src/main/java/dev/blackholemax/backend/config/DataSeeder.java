package dev.blackholemax.backend.config;

import dev.blackholemax.backend.dto.PortfolioFormDTO;
import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.repository.UserRepository;
import dev.blackholemax.backend.service.PortfolioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedDemo(UserRepository userRepository, PortfolioService portfolioService) {
        return args -> {
            // 幂等：演示账号已存在（PostgreSQL 持久化）时跳过，避免重复播种
            if (userRepository.existsByEmail("demo@example.com")) {
                return;
            }
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            User demoUser = new User();
            demoUser.setEmail("demo@example.com");
            demoUser.setPasswordHash(encoder.encode("demo123"));
            demoUser.setDisplayName("Demo User");
            demoUser = userRepository.save(demoUser);

            portfolioService.save(new PortfolioFormDTO(
                    "demo",
                    "Demo User",
                    "让技术，成为我的名片",
                    "热爱技术的独立开发者，专注于 Web 应用与创意编程，用作品说话。",
                    "Java, Spring Boot, AI, 前端",
                    "#4F46E5",
                    "card",
                    true,
                    "/demo/demo-avatar.jpg",
                    "Demo User - 个人作品集 | AI 策展师",
                    "热爱技术的独立开发者，展示 Web 应用与创意编程作品。",
                    null, // generatedHtml
                    null, // customPrompt
                    List.of(
                            "作品集生成器",
                            "极简时钟",
                            "数据可视化面板"
                    ),
                    List.of(
                            "输入个人资料即可生成一个极简风格的作品集页面，本项目就是用它做的。",
                            "一个用 Canvas 手写的极简时钟，支持暗色模式。",
                            "实时渲染指标数据的仪表盘，基于 WebSocket 推送。"
                    ),
                    List.of("/demo/demo-cover-1.jpg", "/demo/demo-cover-2.jpg", "/demo/demo-cover-3.jpg")
            ), demoUser.getId());
        };
    }
}