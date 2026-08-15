package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.AuthRequest;
import dev.blackholemax.backend.dto.RegisterRequest;
import dev.blackholemax.backend.entity.User;

/**
 * 用户认证服务：注册、登录、管理员登录、登出与当前用户查询。
 */
public interface AuthService {

    User register(RegisterRequest request);

    User login(AuthRequest request);

    /** 管理员登录：仅管理员账号允许登录管理后台（admin-web），非管理员返回 403 */
    User loginAdmin(AuthRequest request);

    void logout();

    long currentUserId();

    /** 判断指定用户是否为管理员 */
    boolean isAdmin(long userId);
}
