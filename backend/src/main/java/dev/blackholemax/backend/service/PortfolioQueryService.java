package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.PortfolioPageData;

/**
 * 对外展示页查询服务：按 slug 查询已发布的作品集，结果以 DTO 形式缓存到 "pages" 二级缓存。
 */
public interface PortfolioQueryService {

    /** 未发布或不存在返回 null（不缓存），避免缓存穿透 */
    PortfolioPageData findPublishedBySlug(String slug);
}
