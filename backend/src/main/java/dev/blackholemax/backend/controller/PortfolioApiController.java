package dev.blackholemax.backend.controller;

import dev.blackholemax.backend.dto.PortfolioFormDTO;
import dev.blackholemax.backend.dto.PortfolioPageData;
import dev.blackholemax.backend.entity.Portfolio;
import dev.blackholemax.backend.entity.Project;
import dev.blackholemax.backend.repository.PortfolioRepository;
import dev.blackholemax.backend.repository.VisitRecordRepository;
import dev.blackholemax.backend.service.AuthService;
import dev.blackholemax.backend.service.FileStorage;
import dev.blackholemax.backend.service.PortfolioQueryService;
import dev.blackholemax.backend.service.PortfolioService;
import dev.blackholemax.backend.web.XssFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作品集 JSON 接口（供 Next.js 前端调用）：
 * - /api/admin/portfolios        列表（含 PV/UV）
 * - /api/admin/portfolios/{slug} 编辑回填
 * - POST /api/admin/portfolios   保存（multipart）
 * - /api/p/{slug}                对外展示（已发布）
 * <p>
 * 后台接口落在 /api/admin/**，由 SaTokenConfigure 拦截要求登录；
 * 对外接口 /api/p/** 开放访问，访问计数由 VisitCounterInterceptor 统一记录。
 */
@RestController
public class PortfolioApiController {

    private final PortfolioService portfolioService;
    private final PortfolioQueryService portfolioQueryService;
    private final PortfolioRepository portfolioRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final AuthService authService;
    private final FileStorage fileStorage;

    public PortfolioApiController(PortfolioService portfolioService,
                                  PortfolioQueryService portfolioQueryService,
                                  PortfolioRepository portfolioRepository,
                                  VisitRecordRepository visitRecordRepository,
                                  AuthService authService,
                                  FileStorage fileStorage) {
        this.portfolioService = portfolioService;
        this.portfolioQueryService = portfolioQueryService;
        this.portfolioRepository = portfolioRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.authService = authService;
        this.fileStorage = fileStorage;
    }

    /** 仪表盘列表：当前用户的全部作品集 + PV/UV。 */
    public record PortfolioSummary(
            Long id, String slug, String userName, String slogan, String template,
            boolean isPublished, String themeColor, String avatarPath, long pv, long uv) {
        static PortfolioSummary from(Portfolio p, long pv, long uv) {
            return new PortfolioSummary(p.getId(), p.getSlug(), p.getUserName(), p.getSlogan(),
                    p.getTemplate(), p.isPublished(), p.getThemeColor(), p.getAvatarPath(), pv, uv);
        }
    }

    /** 通用分页响应。 */
    public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    /** 仪表盘统计。 */
    public record PortfolioStats(long total, long published, long totalPv, long totalUv) {}

    @GetMapping("/api/admin/portfolios")
    public PageResult<PortfolioSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        long userId = authService.currentUserId();
        List<Portfolio> portfolios = scopedPortfolios(userId);
        int totalElements = portfolios.size();
        int safeSize = Math.max(size, 1);
        int from = Math.min(page * safeSize, totalElements);
        int to = Math.min(from + safeSize, totalElements);
        List<PortfolioSummary> content = new ArrayList<>();
        for (Portfolio p : portfolios.subList(from, to)) {
            long pv = visitRecordRepository.countByPortfolioId(p.getId());
            long uv = visitRecordRepository.countDistinctVisitorByPortfolioId(p.getId());
            content.add(PortfolioSummary.from(p, pv, uv));
        }
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        return new PageResult<>(content, page, safeSize, totalElements, totalPages);
    }

    /** 仪表盘统计：当前登录者可见范围（管理员=全部，普通用户=本人）的汇总。 */
    @GetMapping("/api/admin/portfolios/stats")
    public PortfolioStats stats() {
        long userId = authService.currentUserId();
        List<Portfolio> portfolios = scopedPortfolios(userId);
        long total = portfolios.size();
        long published = portfolios.stream().filter(Portfolio::isPublished).count();
        long totalPv = 0;
        long totalUv = 0;
        for (Portfolio p : portfolios) {
            totalPv += visitRecordRepository.countByPortfolioId(p.getId());
            totalUv += visitRecordRepository.countDistinctVisitorByPortfolioId(p.getId());
        }
        return new PortfolioStats(total, published, totalPv, totalUv);
    }

    private List<Portfolio> scopedPortfolios(long userId) {
        // 管理员查看全部用户的作品集，普通用户仅查看自己的
        return authService.isAdmin(userId)
                ? portfolioRepository.findAllByOrderByIdDesc()
                : portfolioRepository.findByUserIdOrderByIdDesc(userId);
    }

    /** 编辑回填：在 PortfolioPageData 基础上补 isPublished，便于编辑器渲染复选框。 */
    public record PortfolioEditData(
            String slug, String userName, String slogan, String bio, String skills,
            String themeColor, String template, boolean isPublished,
            String seoTitle, String seoDescription, String avatarPath,
            String generatedHtml, String customPrompt,
            List<PortfolioPageData.ProjectData> projects) {
        static PortfolioEditData from(Portfolio p) {
            // 直接使用 ProjectData 的公开构造器，避免依赖包级私有静态工厂 from()
            List<PortfolioPageData.ProjectData> projects = p.getProjects() == null
                    ? List.of()
                    : p.getProjects().stream()
                            .map(pr -> new PortfolioPageData.ProjectData(
                                    pr.getTitle(), pr.getDescription(), pr.getCoverPath()))
                            .toList();
            return new PortfolioEditData(
                    p.getSlug(), p.getUserName(), p.getSlogan(), p.getBio(), p.getSkills(),
                    p.getThemeColor(), p.getTemplate(), p.isPublished(),
                    p.getSeoTitle(), p.getSeoDescription(), p.getAvatarPath(),
                    p.getGeneratedHtml(), p.getCustomPrompt(), projects);
        }
    }

    @GetMapping("/api/admin/portfolios/{slug}")
    public PortfolioEditData forEdit(@PathVariable String slug) {
        long userId = authService.currentUserId();
        Portfolio p = portfolioRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "作品集不存在"));
        if (!p.getUserId().equals(userId) && !authService.isAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权编辑他人的作品集");
        }
        return PortfolioEditData.from(p);
    }

    /** 保存（新建或按 slug 覆盖更新）。与 AdminController#publish 入参一致，仅改为返回 JSON。 */
    @PostMapping("/api/admin/portfolios")
    public Map<String, String> save(
            @RequestParam String slug,
            @RequestParam String userName,
            @RequestParam(required = false) String slogan,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String themeColor,
            @RequestParam(required = false) String template,
            @RequestParam(name = "isPublished", required = false, defaultValue = "false") boolean isPublished,
            @RequestParam(required = false) String seoTitle,
            @RequestParam(required = false) String seoDescription,
            @RequestParam(required = false) String generatedHtml,
            @RequestParam(required = false) String customPrompt,
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) List<String> title,
            @RequestParam(required = false) List<String> description,
            @RequestParam(required = false) List<MultipartFile> cover) {

        List<String> projectTitles = title == null ? List.of() : title;
        List<String> projectDescriptions = description == null ? List.of() : description;

        List<String> projectCovers = new ArrayList<>();
        if (cover != null) {
            for (MultipartFile file : cover) {
                projectCovers.add(fileStorage.save(file));
            }
        }

        // XSS 过滤：对用户输入的文本字段做 HTML 实体编码（generatedHtml 不过滤，由 AI 生成且在 iframe sandbox 中渲染）
        PortfolioFormDTO dto = new PortfolioFormDTO(
                slug.trim(),
                XssFilter.clean(userName),
                XssFilter.clean(slogan),
                XssFilter.clean(bio),
                XssFilter.clean(skills),
                themeColor,
                template,
                isPublished,
                fileStorage.save(avatar),
                XssFilter.clean(seoTitle),
                XssFilter.clean(seoDescription),
                generatedHtml,
                XssFilter.clean(customPrompt),
                XssFilter.cleanList(projectTitles),
                XssFilter.cleanList(projectDescriptions),
                projectCovers);

        Portfolio saved = portfolioService.save(dto, authService.currentUserId());
        return Map.of("slug", saved.getSlug());
    }

    /**
     * 删除作品集（本人或管理员）。
     */
    @DeleteMapping("/api/admin/portfolios/{slug}")
    public Map<String, String> delete(@PathVariable String slug) {
        long userId = authService.currentUserId();
        Portfolio p = portfolioRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "作品集不存在"));
        if (!p.getUserId().equals(userId) && !authService.isAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除他人的作品集");
        }
        portfolioRepository.delete(p);
        portfolioService.evictPageCache(slug);
        return Map.of("slug", slug, "deleted", "true");
    }

    /**
     * 更新门户 HTML + 表单数据（AI 调整/同步后自动保存，无需提交完整 multipart 表单）。
     * 同时清除页面缓存，确保公共页立即生效。
     */
    @PutMapping("/api/admin/portfolios/{slug}/html")
    public Map<String, String> updateHtml(
            @PathVariable String slug,
            @RequestBody Map<String, Object> body) {
        long userId = authService.currentUserId();
        Portfolio p = portfolioRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "作品集不存在"));
        if (!p.getUserId().equals(userId) && !authService.isAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改他人的作品集");
        }

        // 更新 HTML
        String html = (String) body.get("html");
        if (html != null) {
            p.setGeneratedHtml(html);
        }

        // 更新表单字段（如果提供）
        if (body.containsKey("userName")) p.setUserName((String) body.get("userName"));
        if (body.containsKey("slogan")) p.setSlogan((String) body.get("slogan"));
        if (body.containsKey("bio")) p.setBio((String) body.get("bio"));
        if (body.containsKey("skills")) p.setSkills((String) body.get("skills"));

        // 更新作品项目（如果提供）
        @SuppressWarnings("unchecked")
        List<Map<String, String>> projects = (List<Map<String, String>>) body.get("projects");
        if (projects != null) {
            // 保留旧封面的映射
            List<Project> oldProjects = new ArrayList<>(p.getProjects());
            p.getProjects().clear();
            for (int i = 0; i < projects.size(); i++) {
                Map<String, String> prj = projects.get(i);
                String title = prj.getOrDefault("title", "").trim();
                String desc = prj.getOrDefault("description", "").trim();
                if (title.isEmpty() && desc.isEmpty()) continue;
                Project project = new Project();
                project.setTitle(title.isEmpty() ? "无标题" : title);
                project.setDescription(desc);
                // 保留旧封面（按索引）
                if (i < oldProjects.size()) {
                    project.setCoverPath(oldProjects.get(i).getCoverPath());
                }
                p.addProject(project);
            }
        }

        portfolioRepository.save(p);
        portfolioService.evictPageCache(slug);
        return Map.of("slug", slug, "updated", "true");
    }

    /** 对外展示：已发布的作品集（未发布返回 404）。访问计数由拦截器统一处理。 */
    @GetMapping("/api/p/{slug}")
    public PortfolioPageData view(@PathVariable String slug) {
        PortfolioPageData data = portfolioQueryService.findPublishedBySlug(slug);
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "作品集不存在或未发布");
        }
        return data;
    }
}
