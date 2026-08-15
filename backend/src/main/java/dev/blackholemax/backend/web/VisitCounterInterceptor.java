package dev.blackholemax.backend.web;

import dev.blackholemax.backend.repository.PortfolioRepository;
import dev.blackholemax.backend.service.VisitService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;

/**
 * 前台访问统计：PV 每次 +1，UV 按访客 Cookie(UUID) 去重。
 * 计数通过 @Async 异步执行，不阻塞页面渲染。
 */
@Component
public class VisitCounterInterceptor implements HandlerInterceptor {

    private static final String VISITOR_COOKIE = "visitor_id";

    private final PortfolioRepository portfolioRepository;
    private final VisitService visitService;

    public VisitCounterInterceptor(PortfolioRepository portfolioRepository, VisitService visitService) {
        this.portfolioRepository = portfolioRepository;
        this.visitService = visitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String slug = extractSlug(request.getRequestURI());
        if (slug == null) {
            return true;
        }
        Optional<Long> portfolioId = portfolioRepository.findPublishedIdBySlug(slug);
        if (portfolioId.isEmpty()) {
            return true;
        }
        String visitorId = resolveVisitorId(request, response);
        visitService.recordVisit(portfolioId.get(), visitorId);
        return true;
    }

    private String extractSlug(String uri) {
        // 兼容 Thymeleaf 的 /p/{slug} 与 Next.js 前端的 /api/p/{slug} 两种入口
        if (uri.startsWith("/api/p/") && uri.length() > 7) {
            return uri.substring(7);
        }
        if (uri.startsWith("/p/") && uri.length() > 3) {
            return uri.substring(3);
        }
        return null;
    }

    private String resolveVisitorId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (VISITOR_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        String visitorId = UUID.randomUUID().toString().replace("-", "");
        Cookie cookie = new Cookie(VISITOR_COOKIE, visitorId);
        cookie.setMaxAge(365 * 24 * 3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return visitorId;
    }
}