package dev.blackholemax.backend.entity;

import jakarta.persistence.*;

/**
 * 网站设计风格提示词条目：
 * 从 stylekit.top 提取的 12 大风格分类，每条含关键词、提示词模板与适用场景，
 * 作为 RAG 检索的语料库（TF-IDF 相似度匹配）。
 */
@Entity
@Table(name = "style_prompt")
public class StylePrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 风格唯一标识（如 minimalist, cyberpunk） */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 风格中文名 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 风格描述（用于 RAG 文档内容） */
    @Column(nullable = false, length = 2000)
    private String description;

    /** 关键词（空格分隔，用于 TF-IDF 匹配） */
    @Column(nullable = false, length = 500)
    private String keywords;

    /** 完整提示词模板（含占位符 {{userName}} {{bio}} {{skills}} {{projects}} {{themeColor}}） */
    @Column(nullable = false, length = 4000)
    private String promptTemplate;

    /** 适用场景（简短描述） */
    @Column(length = 500)
    private String useCase;

    public StylePrompt() {
    }

    public StylePrompt(String code, String name, String description, String keywords,
                       String promptTemplate, String useCase) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.keywords = keywords;
        this.promptTemplate = promptTemplate;
        this.useCase = useCase;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }
}
