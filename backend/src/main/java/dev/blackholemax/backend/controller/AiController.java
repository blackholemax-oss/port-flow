package dev.blackholemax.backend.controller;

import dev.blackholemax.backend.dto.AiRequest;
import dev.blackholemax.backend.dto.AiResult;
import dev.blackholemax.backend.entity.StylePrompt;
import dev.blackholemax.backend.service.AiService;
import dev.blackholemax.backend.service.AuthService;
import dev.blackholemax.backend.service.PortalHtmlCacheService;
import dev.blackholemax.backend.service.PortalHtmlService;
import dev.blackholemax.backend.service.StyleRagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
public class AiController {

    private final AiService aiService;
    private final PortalHtmlService portalHtmlService;
    private final StyleRagService styleRagService;
    private final PortalHtmlCacheService htmlCacheService;
    private final AuthService authService;

    public AiController(AiService aiService, PortalHtmlService portalHtmlService,
                        StyleRagService styleRagService, PortalHtmlCacheService htmlCacheService,
                        AuthService authService) {
        this.aiService = aiService;
        this.portalHtmlService = portalHtmlService;
        this.styleRagService = styleRagService;
        this.htmlCacheService = htmlCacheService;
        this.authService = authService;
    }

    @PostMapping("/generate-bio")
    public AiResult.AiBio generateBio(@RequestBody AiRequest.GenerateBioRequest request) {
        return aiService.generateBio(request.keywords());
    }

    @PostMapping("/polish-description")
    public AiResult.AiDescription polishDescription(@RequestBody AiRequest.PolishRequest request) {
        return aiService.polishDescription(request.text(), request.title());
    }

    @PostMapping("/recommend-color")
    public AiResult.AiColor recommendColor(@RequestBody AiRequest.ColorRequest request) {
        return aiService.recommendColor(request.occupation());
    }

    @PostMapping("/generate-seo")
    public AiResult.AiSeo generateSeo(@RequestBody AiRequest.SeoRequest request) {
        return aiService.generateSeo(request.userName(), request.bio());
    }

    /**
     * 生成门户 HTML：
     * 1. 用 RAG 检索风格提示词（或用户自定义提示词）
     * 2. 调用 DeepSeek 生成完整自包含 HTML
     * 3. 校验 HTML 结构，失败重试
     */
    @PostMapping("/generate-portal-html")
    public AiResult.AiPortalHtml generatePortalHtml(@RequestBody Map<String, Object> request) {
        String userName = (String) request.getOrDefault("userName", "");
        String bio = (String) request.getOrDefault("bio", "");
        String skills = (String) request.getOrDefault("skills", "");
        String slogan = (String) request.getOrDefault("slogan", "");
        String themeColor = (String) request.getOrDefault("themeColor", "#4F46E5");
        String userPrompt = (String) request.get("userPrompt");
        String notes = (String) request.get("notes");
        String slug = (String) request.getOrDefault("slug", "draft");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> projects = (List<Map<String, String>>) request.get("projects");

        long userId = authService.currentUserId();
        return portalHtmlService.generatePortalHtml(userName, bio, skills, slogan, themeColor, projects, userPrompt, notes, slug, userId);
    }

    /** 读取缓存在 Redis 的门户 HTML（用户刷新页面后可恢复编辑） */
    @GetMapping("/portal-html")
    public Map<String, String> getCachedPortalHtml(@RequestParam String slug) {
        long userId = authService.currentUserId();
        String html = htmlCacheService.getCached(userId, slug);
        return Map.of("html", html != null ? html : "", "slug", slug);
    }

    /**
     * AI 对话修改：在当前已生成的 HTML 基础上，按用户自然语言指令让 AI 调整。
     * 用户不能直接编辑 HTML 源码，只能通过 AI 对话修改。
     */
    @PostMapping("/adjust-portal-html")
    public AiResult.AiPortalHtml adjustPortalHtml(@RequestBody Map<String, Object> request) {
        String currentHtml = (String) request.getOrDefault("currentHtml", "");
        String instruction = (String) request.getOrDefault("instruction", "");
        String slug = (String) request.getOrDefault("slug", "draft");

        long userId = authService.currentUserId();
        return portalHtmlService.adjustPortalHtml(currentHtml, instruction, slug, userId);
    }

    /** 获取所有可用设计风格（供前端展示风格选择列表） */
    @GetMapping("/styles")
    public List<StylePrompt> listStyles() {
        return styleRagService.allStyles();
    }

    /** RAG 检索：根据输入返回最相关的风格 */
    @PostMapping("/styles/search")
    public List<StylePrompt> searchStyles(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "");
        return styleRagService.retrieve(query, 3);
    }
}