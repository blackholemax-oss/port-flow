package dev.blackholemax.backend.web;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * 全局异常处理：Sa-Token 未登录时，API 返回 401 JSON，页面重定向到登录页。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Object handleNotLogin(NotLoginException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "msg", "未登录或登录已过期"));
        }
        return "redirect:/admin/login";
    }
}