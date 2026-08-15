package dev.blackholemax.backend.config;

import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 管理员账号引导初始化：
 * 启动时读取 app.admin.email / app.admin.password（由环境变量 ADMIN_EMAIL / ADMIN_PASSWORD 注入），
 * 自动创建或提升管理员账号，保证管理后台有可用的管理员入口。
 * 仅当 ADMIN_PASSWORD 非空时生效；为空则跳过（不创建、不修改任何用户）。
 */
@Configuration
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    public CommandLineRunner bootstrapAdmin(UserRepository userRepository,
                                            @Value("${app.admin.email:}") String adminEmail,
                                            @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
            String password = adminPassword == null ? "" : adminPassword.trim();
            if (email.isBlank() || password.isBlank()) {
                log.info("未配置 ADMIN_EMAIL/ADMIN_PASSWORD，跳过管理员账号初始化");
                return;
            }
            if (password.length() < 6) {
                log.warn("ADMIN_PASSWORD 长度不足 6 位，跳过管理员账号初始化");
                return;
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            User user = userRepository.findByEmail(email).orElseGet(User::new);
            boolean isNew = user.getId() == null;
            user.setEmail(email);
            // 幂等：每次启动都以 ADMIN_PASSWORD 重置管理员密码，保证与 .env 配置一致
            user.setPasswordHash(encoder.encode(password));
            user.setDisplayName(user.getDisplayName() == null || user.getDisplayName().isBlank()
                    ? "管理员" : user.getDisplayName());
            user.setAdmin(true);
            userRepository.save(user);
            log.info("管理员账号初始化完成：{}（{}）", email, isNew ? "新建" : "已提升为管理员");
        };
    }
}
