package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.PortfolioFormDTO;
import dev.blackholemax.backend.entity.Portfolio;

/**
 * 作品集保存服务：新建/按 slug 覆盖更新作品集，并清除对应页面缓存。
 */
public interface PortfolioService {

    Portfolio save(PortfolioFormDTO dto, long userId);

    void evictPageCache(String slug);
}
