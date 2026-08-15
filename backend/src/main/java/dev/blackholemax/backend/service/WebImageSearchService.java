package dev.blackholemax.backend.service;

import java.util.List;

/**
 * 网络图片搜索服务：
 * 降级策略：
 * 1. 百度图片搜索（免认证、支持中文关键词、图片量大）
 * 2. Pexels API（需 API key，高质量）
 * 3. LoremFlickr（免认证，最终兜底）
 * 搜索到的图片下载到本地 uploads/ 目录，返回可访问的相对路径。
 */
public interface WebImageSearchService {

    /**
     * 始终启用。
     */
    boolean isEnabled();

    /**
     * 搜索图片并下载到本地。
     *
     * @param query 搜索关键词（中文英文均可）
     * @param count 需要的图片数量
     * @return 下载成功后的图片相对路径列表（如 ["/uploads/xxx.jpg", ...]）
     */
    List<String> searchAndDownload(String query, int count);
}
