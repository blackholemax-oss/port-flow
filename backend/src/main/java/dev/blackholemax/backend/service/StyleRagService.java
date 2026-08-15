package dev.blackholemax.backend.service;

import dev.blackholemax.backend.entity.StylePrompt;

import java.util.List;

/**
 * 设计风格提示词检索服务（轻量 RAG）：
 * 基于 TF-IDF 余弦相似度匹配用户输入与风格提示词库，纯内存计算。
 */
public interface StyleRagService {

    /** 检索与输入最相关的前 K 个风格提示词 */
    List<StylePrompt> retrieve(String input, int topK);

    /** 默认检索 top-3 */
    List<StylePrompt> retrieve(String input);

    /** 获取所有可用风格（供前端展示风格列表） */
    List<StylePrompt> allStyles();

    /** 重建索引（新增风格后调用） */
    void rebuildIndex();
}
