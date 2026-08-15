package dev.blackholemax.backend.service.impl;

import dev.blackholemax.backend.dto.PortfolioFormDTO;
import dev.blackholemax.backend.entity.Portfolio;
import dev.blackholemax.backend.entity.Project;
import dev.blackholemax.backend.repository.PortfolioRepository;
import dev.blackholemax.backend.service.AuthService;
import dev.blackholemax.backend.service.PortalHtmlCacheService;
import dev.blackholemax.backend.service.PortfolioService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 作品集保存服务实现。
 */
@Service
public class PortfolioServiceImpl implements PortfolioService {

    private static final String CACHE_NAME = "pages";

    private final PortfolioRepository portfolioRepository;
    private final CacheManager cacheManager;
    private final PortalHtmlCacheService htmlCacheService;
    private final AuthService authService;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository, CacheManager cacheManager,
                                PortalHtmlCacheService htmlCacheService, AuthService authService) {
        this.portfolioRepository = portfolioRepository;
        this.cacheManager = cacheManager;
        this.htmlCacheService = htmlCacheService;
        this.authService = authService;
    }

    /**
     * 保存（新建或按 slug 覆盖更新）作品集，并立即清除该 slug 对应的页面缓存。
     * 多租户校验：普通用户仅能操作自己的作品集；管理员可管理所有用户的作品集，
     * 编辑他人作品集时保留原归属，不改变 owner。
     */
    @Override
    @Transactional
    public Portfolio save(PortfolioFormDTO dto, long userId) {
        Optional<Portfolio> existing = portfolioRepository.findBySlug(dto.slug());
        Portfolio portfolio = existing.orElseGet(Portfolio::new);
        boolean isOwner = existing.isPresent() && existing.get().getUserId().equals(userId);
        if (existing.isPresent() && !isOwner && !authService.isAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改他人的作品集");
        }

        portfolio.setSlug(dto.slug());
        // 新建时归属当前用户；编辑他人作品集（管理员）时保留原 owner
        if (existing.isEmpty()) {
            portfolio.setUserId(userId);
        } else {
            portfolio.setUserId(existing.get().getUserId());
        }
        portfolio.setUserName(dto.userName());
        portfolio.setSlogan(dto.slogan());
        portfolio.setBio(dto.bio());
        portfolio.setSkills(dto.skills());
        portfolio.setThemeColor(dto.themeColor());
        portfolio.setTemplate(dto.template());
        portfolio.setSeoTitle(dto.seoTitle());
        portfolio.setSeoDescription(dto.seoDescription());
        portfolio.setPublished(dto.isPublished());

        // AI 生成门户 HTML：template='custom' 时使用，其他模板清空
        if ("custom".equals(dto.template())) {
            portfolio.setGeneratedHtml(dto.generatedHtml());
        } else {
            portfolio.setGeneratedHtml(null);
        }
        portfolio.setCustomPrompt(dto.customPrompt());

        // 未上传新头像时保留原头像
        if (dto.avatarPath() != null) {
            portfolio.setAvatarPath(dto.avatarPath());
        }

        List<Project> oldProjects = existing.map(Portfolio::getProjects).orElse(List.of());
        List<String> titles = dto.projectTitles() == null ? List.of() : dto.projectTitles();
        List<String> descriptions = dto.projectDescriptions() == null ? List.of() : dto.projectDescriptions();
        List<String> covers = dto.projectCovers() == null ? List.of() : dto.projectCovers();

        List<Project> projects = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i) == null ? "" : titles.get(i).trim();
            String cover = i < covers.size() ? covers.get(i) : null;
            if (title.isEmpty() && (cover == null || cover.isEmpty())) {
                continue;
            }
            Project project = new Project();
            project.setTitle(title);
            project.setDescription(i < descriptions.size() ? descriptions.get(i) : null);
            // 未上传新封面时按索引保留旧封面
            if (cover == null && i < oldProjects.size()) {
                cover = oldProjects.get(i).getCoverPath();
            }
            project.setCoverPath(cover);
            projects.add(project);
        }

        portfolio.getProjects().clear();
        projects.forEach(portfolio::addProject);

        Portfolio saved = portfolioRepository.save(portfolio);
        evictPageCache(dto.slug());
        // 清除 Redis 中缓存的临时 HTML（已持久化到 DB）
        htmlCacheService.evict(userId, dto.slug());
        return saved;
    }

    /**
     * 手动清除 slug 对应的页面缓存，保证重新发布后页面实时更新。
     */
    @Override
    public void evictPageCache(String slug) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(slug);
        }
    }
}
