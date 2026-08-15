package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.AdminUserRequest;

import java.util.List;

/**
 * 后台用户管理服务：管理员对平台用户的增删改查。
 * 业务约束（防误操作）：
 * - 不能修改 / 删除当前登录账号自身
 * - 系统至少保留一名管理员
 * - 存在作品集的用户不允许直接删除
 */
public interface AdminUserService {

    /** 用户摘要（不包含密码等敏感字段） */
    record UserSummary(Long id, String email, String displayName, boolean admin, String createdAt) {
    }

    /** 全部用户列表（按创建时间倒序） */
    List<UserSummary> list();

    /** 用户详情 */
    UserSummary get(Long id);

    /** 创建用户（admin-web 管理后台新建账号） */
    UserSummary create(AdminUserRequest request);

    /** 更新用户（昵称 / 密码 / 管理员权限） */
    UserSummary update(Long id, AdminUserRequest request);

    /** 删除用户 */
    void delete(Long id);
}
