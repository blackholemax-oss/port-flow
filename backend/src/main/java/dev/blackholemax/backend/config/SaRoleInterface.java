package dev.blackholemax.backend.config;

import cn.dev33.satoken.stp.StpInterface;
import dev.blackholemax.backend.entity.User;
import dev.blackholemax.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 角色数据源：从数据库实时读取用户角色。
 * 管理员用户返回 ["admin"]，用于后台管理接口（/api/admin/users/**）的权限校验。
 */
@Component
public class SaRoleInterface implements StpInterface {

    private final UserRepository userRepository;

    public SaRoleInterface(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return userRepository.findById(Long.valueOf(loginId.toString()))
                .filter(User::isAdmin)
                .map(u -> List.of("admin"))
                .orElse(List.of());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }
}
