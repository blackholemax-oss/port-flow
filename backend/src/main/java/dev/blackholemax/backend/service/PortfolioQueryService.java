package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.PortfolioPageData;
import dev.blackholemax.backend.entity.Portfolio;
import dev.blackholemax.backend.repository.PortfolioRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 对外展示页查询：按 slug 查询已发布的作品集，结果以 DTO 形式缓存到 "pages" 二级缓存。
 * 未发布或不存在返回 null（不缓存），避免缓存穿透。
 */
@Service
public class PortfolioQueryService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioQueryService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Cacheable(value = "pages", key = "#slug", unless = "#result == null")
    public PortfolioPageData findPublishedBySlug(String slug) {
        return portfolioRepository.findBySlug(slug)
                .filter(Portfolio::isPublished)
                .map(PortfolioPageData::from)
                .orElse(null);
    }
}