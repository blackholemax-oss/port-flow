package dev.blackholemax.backend.service.impl;

import dev.blackholemax.backend.dto.PortfolioPageData;
import dev.blackholemax.backend.entity.Portfolio;
import dev.blackholemax.backend.service.PortfolioQueryService;
import dev.blackholemax.backend.repository.PortfolioRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 对外展示页查询服务实现：按 slug 查询已发布的作品集，结果以 DTO 形式缓存到 "pages" 二级缓存。
 */
@Service
public class PortfolioQueryServiceImpl implements PortfolioQueryService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioQueryServiceImpl(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    @Cacheable(value = "pages", key = "#slug", unless = "#result == null")
    public PortfolioPageData findPublishedBySlug(String slug) {
        return portfolioRepository.findBySlug(slug)
                .filter(Portfolio::isPublished)
                .map(PortfolioPageData::from)
                .orElse(null);
    }
}
