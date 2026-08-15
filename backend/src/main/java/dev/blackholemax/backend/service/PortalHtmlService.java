package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.AiResult;

import java.util.List;
import java.util.Map;

/**
 * 门户 HTML 生成服务：
 * 1. 用 RAG 检索相关风格提示词
 * 2. 构建严格的 system prompt（要求输出完整自包含 HTML）
 * 3. 调用 DeepSeek 生成 HTML
 * 4. 校验 HTML 结构（必须含 <html> <body>，必须自包含）
 * 5. 校验失败则修正 prompt 后重试（最多 2 次）
 * 6. 生成的 HTML 缓存到 Redis（供编辑模式读取）
 */
public interface PortalHtmlService {

    /**
     * 生成门户 HTML：
     * @param userName 用户名
     * @param bio 个人简介
     * @param skills 技能（逗号分隔）
     * @param slogan 口号
     * @param themeColor 主题色
     * @param projects 项目列表 [{title, description}]
     * @param userPrompt 用户自定义提示词（可为空，为空时用 RAG 检索风格）
     * @param notes 灵感便签（用户输入的关键词/灵感描述，用于图片搜索）
     * @param slug 页面地址（用于缓存键）
     * @return 生成的完整 HTML 字符串
     */
    AiResult.AiPortalHtml generatePortalHtml(
            String userName, String bio, String skills, String slogan,
            String themeColor, List<Map<String, String>> projects,
            String userPrompt, String notes, String slug, long userId);

    /**
     * AI 对话修改：在当前已生成的 HTML 基础上，按用户自然语言指令让 AI 调整。
     * 不使用降级模板——失败时返回原 HTML，保证用户已生成的内容不丢失。
     */
    AiResult.AiPortalHtml adjustPortalHtml(
            String currentHtml, String instruction, String slug, long userId);
}
