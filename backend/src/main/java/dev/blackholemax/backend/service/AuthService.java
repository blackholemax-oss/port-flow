package dev.blackholemax.backend.service;

import cn.dev33.satoken.stp.StpUtil;
import dev.blackholemax.backend.dto.AuthRequest;
import dev.blackholemax.backend.dto.RegisterRequest;
import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

    public void logout() {
        StpUtil.logout();
    }

    public long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}