package dev.blackholemax.backend.controller;

import dev.blackholemax.backend.dto.AuthRequest;
import dev.blackholemax.backend.dto.RegisterRequest;
import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return Map.of("code", 200, "userId", user.getId(), "email", user.getEmail());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        User user = authService.login(request);
        return Map.of("code", 200, "userId", user.getId(), "email", user.getEmail());
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        authService.logout();
        return Map.of("code", 200, "msg", "已退出登录");
    }
}