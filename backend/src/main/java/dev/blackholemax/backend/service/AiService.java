package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.AiResult;

/**
 * AI 智能引擎接口：写 Bio、润色描述、推荐配色、生成 SEO。
 * 默认实现为 MockAiServiceImpl（规则模板），配置 ai.deepseek.api-key 后由 DeepSeekAiServiceImpl 接管。
 */
public interface AiService {

    /**
     * 根据碎片化关键词生成结构化 Bio（Slogan + 故事 + 技能标签）。
     */
    AiResult.AiBio generateBio(String keywords);

    /**
     * 将口语化的原始描述润色为专业作品描述。
     * @param raw 原始描述
     * @param title 作品标题（提供上下文，可为 null）
     */
    AiResult.AiDescription polishDescription(String raw, String title);

    /**
     * 根据职业属性推荐主题色。
     */
    AiResult.AiColor recommendColor(String occupation);

    /**
     * 根据用户资料生成页面 Title 与 Meta Description。
     */
    AiResult.AiSeo generateSeo(String userName, String bio);
}