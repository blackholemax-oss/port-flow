package dev.blackholemax.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.blackholemax.backend.dto.AuthRequest;
import dev.blackholemax.backend.dto.RegisterRequest;
import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.repository.UserRepository;
import dev.blackholemax.backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 用户认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(RegisterRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        if (email.isBlank() || request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱或密码不合法（密码至少 6 位）");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? email.substring(0, email.indexOf('@')) : request.displayName().trim());
        user = userRepository.save(user);
        StpUtil.login(user.getId());
        return user;
    }

    @Override
    public User login(AuthRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误"));
        if (!passwordEncoder.matches(request.password() == null ? "" : request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
        }
        StpUtil.login(user.getId());
        return user;
    }

    @Override
    public User loginAdmin(AuthRequest request) {
        User user = login(request);
        if (!user.isAdmin()) {
            // 非管理员不允许登录管理后台：登出本次会话后返回 403
            StpUtil.logout();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问管理后台");
        }
        return user;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public boolean isAdmin(long userId) {
        return userRepository.findById(userId).map(User::isAdmin).orElse(false);
    }
}
