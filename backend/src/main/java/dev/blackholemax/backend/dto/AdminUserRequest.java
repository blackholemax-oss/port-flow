package dev.blackholemax.backend.dto;

/**
 * 后台用户管理请求：创建 / 更新用户。
 * 创建时 email、password 必填；更新时仅非 null 字段生效。
 */
public record AdminUserRequest(String email, String password, String displayName, Boolean admin) {
}
