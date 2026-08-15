package dev.blackholemax.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.blackholemax.backend.dto.AdminUserRequest;
import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.repository.PortfolioRepository;
import dev.blackholemax.backend.repository.UserRepository;
import dev.blackholemax.backend.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * 后台用户管理服务实现。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminUserServiceImpl(UserRepository userRepository, PortfolioRepository portfolioRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public List<UserSummary> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    @Override
    public UserSummary get(Long id) {
        return toSummary(requireUser(id));
    }

    @Override
    public UserSummary create(AdminUserRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
        }
        String email = request.email().trim().toLowerCase();
        if (request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少 6 位");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? email.substring(0, email.indexOf('@')) : request.displayName().trim());
        user.setAdmin(request.admin() != null && request.admin());
        return toSummary(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserSummary update(Long id, AdminUserRequest request) {
        User user = requireUser(id);
        if (request == null) {
            return toSummary(user);
        }
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少 6 位");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.admin() != null && request.admin() != user.isAdmin()) {
            long currentUserId = StpUtil.getLoginIdAsLong();
            if (user.getId().equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能修改自己的管理员权限");
            }
            if (user.isAdmin() && !request.admin() && userRepository.countByAdminTrue() <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "系统至少保留一名管理员");
            }
            user.setAdmin(request.admin());
        }
        return toSummary(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = requireUser(id);
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (user.getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能删除当前登录账号");
        }
        if (user.isAdmin() && userRepository.countByAdminTrue() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "系统至少保留一名管理员");
        }
        if (!portfolioRepository.findByUserIdOrderByIdDesc(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户存在作品集，请先删除其作品集");
        }
        userRepository.delete(user);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(), user.getEmail(), user.getDisplayName(),
                user.isAdmin(), user.getCreatedAt() == null ? "" : user.getCreatedAt().toString());
    }
}
