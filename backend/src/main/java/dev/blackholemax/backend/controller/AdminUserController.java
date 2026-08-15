package dev.blackholemax.backend.controller;

import dev.blackholemax.backend.dto.AdminUserRequest;
import dev.blackholemax.backend.service.AdminUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台用户管理接口（仅管理员角色可访问，见 SaTokenConfigure 中 checkRole("admin")）：
 * GET    /api/admin/users        用户列表
 * GET    /api/admin/users/{id}   用户详情
 * POST   /api/admin/users        创建用户
 * PUT    /api/admin/users/{id}   更新用户
 * DELETE /api/admin/users/{id}   删除用户
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserService.UserSummary> list() {
        return adminUserService.list();
    }

    @GetMapping("/{id}")
    public AdminUserService.UserSummary get(@PathVariable Long id) {
        return adminUserService.get(id);
    }

    @PostMapping
    public AdminUserService.UserSummary create(@RequestBody AdminUserRequest request) {
        return adminUserService.create(request);
    }

    @PutMapping("/{id}")
    public AdminUserService.UserSummary update(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        return adminUserService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Map.of("code", 200, "deleted", id);
    }
}
