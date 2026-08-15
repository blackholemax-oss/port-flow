package dev.blackholemax.backend.service;

import dev.blackholemax.backend.dto.AiResult;
import dev.blackholemax.backend.entity.StylePrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
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
@Service
public class PortalHtmlService {

    private static final Logger log = LoggerFactory.getLogger(PortalHtmlService.class);
    private static final int MAX_RETRIES = 2;

    /**
     * 图片搜索关键词：primary 优先用于搜索（作品名优先），secondary 为补充视觉词。
     * needImages 表示用户是否明确要求图片应用到页面（false 时不搜索图片）。
     */
    private record ImageKeywords(String primary, String secondary, boolean needImages) {}

    /**
     * 图片搜索结果：包含下载到本地的图片路径和实际使用的关键词。
     */
    private record ImageSearchResult(List<String> urls, ImageKeywords keywords) {}

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StyleRagService styleRagService;
    private final MockAiServiceImpl fallback;
    private final PortalHtmlCacheService htmlCacheService;
    private final WebImageSearchService imageSearchService;
    private final String modelName;

    public PortalHtmlService(
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl,
            @Value("${ai.deepseek.model}") String model,
            @Value("${ai.deepseek.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper,
            StyleRagService styleRagService,
            MockAiServiceImpl fallback,
            PortalHtmlCacheService htmlCacheService,
            WebImageSearchService imageSearchService) {
        this.objectMapper = objectMapper;
        this.styleRagService = styleRagService;
        this.fallback = fallback;
        this.htmlCacheService = htmlCacheService;
        this.imageSearchService = imageSearchService;
        this.modelName = model;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
    }

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
    public AiResult.AiPortalHtml generatePortalHtml(
            String userName, String bio, String skills, String slogan,
            String themeColor, List<Map<String, String>> projects,
            String userPrompt, String notes, String slug, long userId) {

        // 1. 确定使用哪种提示词来源
        String styleContext;
        if (userPrompt != null && !userPrompt.isBlank()) {
            // 用户自定义提示词：仍用 RAG 检索补充风格上下文
            List<StylePrompt> retrieved = styleRagService.retrieve(userPrompt, 2);
            styleContext = buildUserPromptContext(userPrompt, retrieved);
        } else {
            // 无自定义提示词：用 RAG 检索风格
            String queryText = (bio != null ? bio : "") + " " + (skills != null ? skills : "") + " " + (slogan != null ? slogan : "");
            List<StylePrompt> retrieved = styleRagService.retrieve(queryText, 3);
            styleContext = buildRagContext(retrieved);
        }

        // 1.5 网络图片搜索：用 AI 从用户输入（灵感便签+提示词）中提取关键词搜索图片
        // 关键词提取只看 notes + userPrompt，剔除 RAG styleContext 防止污染
        ImageSearchResult imageSearchResult = searchImagesForPortal(userPrompt, notes, skills);
        List<String> imageUrls = imageSearchResult.urls();
        if (!imageUrls.isEmpty()) {
            log.info("已搜索到 {} 张素材图片，将提供给 AI 使用", imageUrls.size());
        }

        // 关键词描述（透传给前端，便于用户判断图片搜索是否精准）
        String imageKeywordsDesc = formatImageKeywords(imageSearchResult.keywords(), imageUrls.size());

        // 2. 构建 system prompt（严格约束输出格式，包含图片素材信息）
        String systemPrompt = buildSystemPrompt(styleContext, imageUrls);

        // 3. 构建 user prompt（含用户数据）
        String userContent = buildUserContent(userName, bio, skills, slogan, themeColor, projects);

        // 4. 调用 LLM（带重试）
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String content = callLlm(systemPrompt, userContent, attempt > 0);
                String html = extractHtml(content);

                if (validateHtml(html)) {
                    log.info("门户 HTML 生成成功（第 {} 次尝试）", attempt + 1);
                    // 缓存到 Redis，便于用户编辑修改
                    htmlCacheService.cache(userId, slug, html);
                    return new AiResult.AiPortalHtml(html, "生成成功，可继续编辑或发布", imageKeywordsDesc);
                } else {
                    log.warn("HTML 校验失败（第 {} 次尝试），重试中...", attempt + 1);
                    if (attempt == MAX_RETRIES) {
                        // 最后一次仍失败，返回降级 HTML
                        String fallbackHtml = buildFallbackHtml(userName, bio, skills, slogan, themeColor, projects);
                        return new AiResult.AiPortalHtml(fallbackHtml, "AI 生成的内容未通过格式检查，已使用备用模板", imageKeywordsDesc);
                    }
                }
            } catch (Exception e) {
                log.warn("LLM 调用失败（第 {} 次尝试）：{}", attempt + 1, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    String fallbackHtml = buildFallbackHtml(userName, bio, skills, slogan, themeColor, projects);
                    return new AiResult.AiPortalHtml(fallbackHtml, "AI 服务暂时不可用，已使用备用模板", imageKeywordsDesc);
                }
            }
        }

        String fallbackHtml = buildFallbackHtml(userName, bio, skills, slogan, themeColor, projects);
        htmlCacheService.cache(userId, slug, fallbackHtml);
        return new AiResult.AiPortalHtml(fallbackHtml, "生成未成功，已使用备用模板", imageKeywordsDesc);
    }

    /**
     * 格式化图片关键词为可读字符串，透传给前端展示。
     * 例：primary=从零开始的异世界生活, secondary=动漫 银发, count=4
     * → "已用关键词「从零开始的异世界生活 / 动漫 银发」搜索 4 张图片"
     */
    private String formatImageKeywords(ImageKeywords keywords, int count) {
        if (keywords == null) return "";
        String primary = keywords.primary();
        String secondary = keywords.secondary();
        if (primary == null || primary.isBlank()) return "";
        String joined = secondary != null && !secondary.isBlank()
                ? primary + " / " + secondary
                : primary;
        if (count > 0) {
            return "已用关键词「" + joined + "」搜索 " + count + " 张图片";
        }
        return "图片关键词：「" + joined + "」";
    }

    /**
     * AI 对话修改：在当前已生成的 HTML 基础上，按用户自然语言指令让 AI 调整。
     * 不使用降级模板——失败时返回原 HTML，保证用户已生成的内容不丢失。
     */
    public AiResult.AiPortalHtml adjustPortalHtml(
            String currentHtml, String instruction, String slug, long userId) {

        if (currentHtml == null || currentHtml.isBlank()) {
            return new AiResult.AiPortalHtml("", "请先生成网页，再让 AI 调整", null);
        }
        if (instruction == null || instruction.isBlank()) {
            return new AiResult.AiPortalHtml(currentHtml, "请输入想要调整的内容", null);
        }

        String systemPrompt = """
                你是资深前端工程师。用户已经有一个完整的个人作品集门户网页（HTML），现在想对它做一些调整。
                你需要严格按照用户的修改指令，在现有 HTML 的基础上做修改。

                【输出格式（最高优先级）】
                - 你的回复必须仅包含修改后的完整 HTML 源码本身
                - 第一个字符必须是 '<'，最后一个字符必须是 '>'
                - 第一个词必须是 `<!DOCTYPE html>`，最后一个标签必须是 `</html>`
                - 严禁输出 markdown 代码块标记（不要 ```html，不要 ```）
                - 严禁输出任何说明文字、解释、问候
                - 必须输出完整的 HTML 文档（不能只输出 diff 或片段）

                【修改原则】
                1. 用户指令要求修改什么，你就修改什么，必须严格执行
                2. 如果用户指令要求更新文字内容（如姓名、口号、简介、技能、作品项目），必须按指令提供的内容更新网页中对应的区块
                3. 如果用户指令要求添加新的作品项目，必须在网页的作品项目区块中新增对应内容，保持与其他作品项目相同的 HTML 结构和样式
                4. 如果用户指令要求删除某些内容，必须删除对应的 HTML 元素
                5. 用户指令未提及的内容保持原样，不要擅自修改
                6. 不要编造用户指令中没有提到的内容

                【内容约束】
                1. 所有 CSS 必须内联在 <style> 标签中，所有 JS 必须内联在 <script> 标签中
                2. 不得引用任何外部 JS/CSS 资源
                3. 保持网页的整体设计风格和配色不变（除非用户明确要求修改样式）
                4. 保持响应式布局
                5. 控制 HTML 体积在 6000 token 以内
                6. 所有内容必须默认可见——严禁使用 opacity:0 隐藏内容，严禁用 IntersectionObserver 等 JS 控制可见性
                7. 如需动画用纯 CSS（@keyframes），动画结束后 opacity 必须为 1

                记住：直接以 `<!DOCTYPE html>` 开头输出完整 HTML，不要任何其他文字。严格按照用户指令执行修改。
                """;

        String userContent = """
                以下是我当前的门户网页 HTML：

                ```html
                %s
                ```

                我的修改要求： %s

                请输出修改后的完整 HTML。
                """.formatted(currentHtml, instruction);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String content = callLlm(systemPrompt, userContent, attempt > 0);
                String html = extractHtml(content);

                if (validateHtml(html)) {
                    log.info("门户 HTML 调整成功（第 {} 次尝试）", attempt + 1);
                    htmlCacheService.cache(userId, slug, html);
                    return new AiResult.AiPortalHtml(html, "调整完成", null);
                } else {
                    log.warn("调整后 HTML 校验失败（第 {} 次尝试），重试中...", attempt + 1);
                    if (attempt == MAX_RETRIES) {
                        // 调整失败时返回原 HTML，不丢失用户已生成的内容
                        return new AiResult.AiPortalHtml(currentHtml, "AI 调整未成功，已保留原网页", null);
                    }
                }
            } catch (Exception e) {
                log.warn("LLM 调用失败（第 {} 次尝试）：{}", attempt + 1, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    return new AiResult.AiPortalHtml(currentHtml, "AI 服务暂时不可用，已保留原网页", null);
                }
            }
        }

        return new AiResult.AiPortalHtml(currentHtml, "调整未成功，已保留原网页", null);
    }

    private String buildSystemPrompt(String styleContext, List<String> imageUrls) {
        String imageSection = "";
        if (imageUrls != null && !imageUrls.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n【素材图片（已下载到本地，必须按以下路径作为 <img src> 使用）】\n");
            for (int i = 0; i < imageUrls.size(); i++) {
                sb.append(i + 1).append(". ").append(imageUrls.get(i)).append("\n");
            }
            sb.append("\n【图片使用规则（必须严格遵守）】\n");
            sb.append("1. 必须使用以上路径（原样复制，不要修改路径）作为 <img src> 或 CSS background-image url()\n");
            sb.append("2. 路径格式必须为 /uploads/xxx.jpg（相对路径，前端会自动代理到后端，不要加 host）\n");
            sb.append("3. 第 1 张图片作为 Hero 区域的背景图或主视觉图，必须明显可见\n");
            sb.append("4. 其余图片作为作品封面或装饰画廊，每张图片必须清晰显示\n");
            sb.append("5. 严禁用深色遮罩盖住图片（如 rgba(0,0,0,0.5) 以上不透明度）；如需叠加文字，遮罩不透明度不得超过 0.4\n");
            sb.append("6. 严禁使用 opacity:0 隐藏图片，严禁 display:none 隐藏图片\n");
            sb.append("7. 图片必须出现在页面可见区域，不能放在折叠/隐藏区域\n");
            imageSection = sb.toString();
        }

        return """
                你是资深前端设计师 + 全栈工程师。根据用户提供的个人信息和设计风格提示词，生成一个完整的、自包含的个人作品集门户网页。

                【输出格式（最高优先级，必须严格遵守）】
                - 你的回复必须仅包含 HTML 源码本身，第一个字符必须是 '<'，最后一个字符必须是 '>'
                - 第一个词必须是 `<!DOCTYPE html>`，最后一个标签必须是 `</html>`
                - 严禁输出 markdown 代码块标记（不要 ```html，不要 ```）
                - 严禁输出任何说明文字、问候、解释、前后缀（如"好的，这是您的网页："、"希望您喜欢"等）
                - 严禁输出 JSON、注释外的东西或思考过程
                - 如果违反上述任一条，结果将被丢弃

                【基础信息准确性（最高优先级，必须严格遵守）】
                用户在下方"用户信息"中提供的姓名、口号、简介、技能、作品项目等基础信息，你必须在网页中原样使用，严禁以下行为：
                1. 严禁修改、缩写、编造或替换用户的姓名、口号、简介内容
                2. 严禁增删或修改技能标签的内容（用户写"Java, Spring Boot"就必须原样展示这两个）
                3. 严禁编造用户没有提供的作品项目；用户提供的作品标题和描述必须原样展示
                4. 严禁更改主题色色值（用户指定 #4F46E5 就必须用这个值）
                5. 如果用户信息中某个字段为空，对应区块留空或显示占位提示，不要编造内容填充
                6. 你可以优化排版和视觉呈现，但文字内容必须与用户输入完全一致

                【内容约束】
                1. 所有 CSS 必须内联在 <style> 标签中，所有 JS 必须内联在 <script> 标签中
                2. 不得引用任何外部 JS/CSS 资源
                3. 如果提供了素材图片，必须使用它们的路径作为 <img src>；未提供图片时用纯 CSS 绘制装饰元素
                4. 不得使用 JSX、React、Vue 等框架语法，必须是纯 HTML/CSS/JS
                5. 网页必须响应式，适配桌面和移动端（用 media query）
                6. 必须包含以下区块：头像区、姓名、口号、个人简介、技能标签、作品项目列表、页脚
                7. 主题色必须应用到关键视觉元素（标题强调、按钮、标签等）
                8. 控制 HTML 体积在 6000 token 以内，避免被截断

                【动画与可见性约束（必须严格遵守）】
                1. 所有内容必须默认可见——严禁使用 opacity:0、display:none、visibility:hidden 等隐藏初始内容
                2. 严禁使用 IntersectionObserver、scroll 事件监听等 JS 控制元素可见性
                3. 如需动画效果，必须使用纯 CSS 动画（如 @keyframes），且动画结束后元素必须可见（opacity:1）
                4. 示例：可用 `animation: fadeIn 0.8s ease-out forwards` 替代 JS 淡入，但不要设置初始 opacity:0

                【设计风格参考】
                %s
                %s

                记住：直接以 `<!DOCTYPE html>` 开头输出 HTML，不要任何其他文字。用户的基础信息必须原样使用，不得编造或修改。
                """.formatted(styleContext, imageSection);
    }

    /**
     * 根据用户提示词搜索图片。
     * 先用 DeepSeek 从用户自然语言输入中提取精准的图片搜索关键词（结构化 JSON），
     * 再用 primary 关键词优先搜索，结果不足时用 secondary 补充。
     * 关键词提取只看 notes + userPrompt，剔除 RAG styleContext 防止污染。
     * needImages=false 时跳过搜索（用户未明确要求图片应用）。
     */
    private ImageSearchResult searchImagesForPortal(String userPrompt, String notes, String skills) {
        if (!imageSearchService.isEnabled()) {
            return new ImageSearchResult(List.of(), new ImageKeywords("", "", false));
        }

        // 用 AI 提取图片搜索关键词（结构化输出，作品名优先）+ needImages 判断
        ImageKeywords keywords = extractImageKeywordsWithAI(userPrompt, notes, skills);
        log.info("图片搜索关键词：primary={}, secondary={}, needImages={}",
                keywords.primary(), keywords.secondary(), keywords.needImages());

        // 用户未明确要求图片应用到页面 → 跳过搜索
        if (!keywords.needImages()) {
            log.info("用户未明确要求图片应用到页面，跳过图片搜索");
            return new ImageSearchResult(List.of(), keywords);
        }

        // 主关键词（优先作品名）搜索，目标 4 张
        List<String> images = new ArrayList<>(imageSearchService.searchAndDownload(keywords.primary(), 4));

        // 结果不足 3 张时，用次关键词（视觉词）补充
        if (images.size() < 3 && !keywords.secondary().isBlank()) {
            log.info("主关键词结果不足（{}张），用次关键词补充搜索：{}", images.size(), keywords.secondary());
            images.addAll(imageSearchService.searchAndDownload(keywords.secondary(), 4 - images.size()));
        }

        return new ImageSearchResult(images, keywords);
    }

    /**
     * 用 DeepSeek 从用户自然语言输入中提取图片搜索关键词（结构化 JSON 输出）。
     * 用户可能输入"基于从零开始的异世界生活这部动漫的风格"，
     * AI 应提取 primary="从零开始的异世界生活"，secondary="动漫 插画"。
     * 同时判断 needImages：用户是否明确要求"把图片应用到页面"。
     * AI 调用失败时降级为简单截取。
     */
    private ImageKeywords extractImageKeywordsWithAI(String userPrompt, String notes, String skills) {
        // 输入只包含 notes + userPrompt，剔除 RAG styleContext 防止污染
        StringBuilder inputText = new StringBuilder();
        if (notes != null && !notes.isBlank()) {
            inputText.append(notes.trim());
        }
        if (userPrompt != null && !userPrompt.isBlank()) {
            if (inputText.length() > 0) inputText.append("，");
            inputText.append(userPrompt.trim());
        }
        if (inputText.length() == 0) {
            if (skills != null && !skills.isBlank()) {
                return new ImageKeywords(skills.split(",")[0].trim(), "", false);
            }
            return new ImageKeywords("个人作品集", "", false);
        }

        try {
            String content = callDeepSeek(
                    """
                    你是图片搜索关键词提取专家。从用户输入中提取"图片搜索关键词"，并判断是否需要搜索图片。

                    【识别优先级】
                    1. 第一优先：识别用户提到的具体作品名（动漫/电影/游戏/小说），必须使用作品完整原名，不得拆分、不得翻译、不得改写
                       - "从零开始的异世界生活" → primary="从零开始的异世界生活"（不可拆成"从零开始 异世界"）
                       - "鬼灭之刃 刀匠村" → primary="鬼灭之刃"
                       - "Re:Zero" → primary="Re:Zero 从零开始的异世界生活"
                    2. 第二优先：若无作品名，提取 2-3 个视觉关键词（中文），用空格分隔
                       - "科技风格 深蓝色 极简" → primary="科技 深蓝 极简"

                    【needImages 判断规则】
                    - true：用户明确要求"把图片应用到页面""配图""用网络图片""应用图片""插入图片""图片素材""Hero 图"等
                    - false：用户只描述设计风格/作品名，未明确要求图片应用到页面（如"基于动漫风格生成页面"）

                    【输出格式】严格输出 JSON，不要任何其他文字、引号或代码块标记：
                    {"primary":"作品名或核心视觉词","secondary":"辅助视觉词，可为空","needImages":true或false}

                    primary 必须是最能搜到对图的词（优先作品名）；secondary 是补充视觉词（如"动漫 插画 海报"或具体角色名）。
                    """,
                    "用户输入：" + inputText
            );
            String json = content.trim();
            // 去除可能的 markdown 代码块标记（LLM 偶尔不听话）
            json = json.replaceAll("(?s)```(?:json)?\\s*", "").replaceAll("\\s*```", "").trim();
            // 提取最外层 JSON 对象（处理 LLM 输出前后多余文字的情况）
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            JsonNode node = objectMapper.readTree(json);
            String primary = node.path("primary").asText("").replaceAll("[\"'\\n\\r]", "").trim();
            String secondary = node.path("secondary").asText("").replaceAll("[\"'\\n\\r]", "").trim();
            boolean needImages = node.path("needImages").asBoolean(false);

            if (primary.isBlank() || primary.length() > 50) {
                log.warn("AI 关键词提取结果异常 [primary={}, secondary={}, needImages={}]，降级",
                        primary, secondary, needImages);
                return fallbackExtractKeywords(userPrompt, skills);
            }
            return new ImageKeywords(primary, secondary, needImages);
        } catch (Exception e) {
            log.warn("AI 关键词提取失败，降级为简单截取：{}", e.getMessage());
            return fallbackExtractKeywords(userPrompt, skills);
        }
    }

    /**
     * 降级关键词提取：从用户输入中简单截取。
     * 降级时 needImages 默认 false（保守策略，避免误搜图）。
     */
    private ImageKeywords fallbackExtractKeywords(String userPrompt, String skills) {
        String primary;
        if (userPrompt != null && !userPrompt.isBlank()) {
            primary = userPrompt.trim();
            if (primary.length() > 20) {
                primary = primary.substring(0, 20);
            }
        } else if (skills != null && !skills.isBlank()) {
            primary = skills.split(",")[0].trim();
        } else {
            primary = "个人作品集";
        }
        return new ImageKeywords(primary, "", false);
    }

    private String buildUserContent(String userName, String bio, String skills, String slogan,
                                     String themeColor, List<Map<String, String>> projects) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下用户信息生成个人作品集门户网页。\n\n");
        sb.append("【用户信息（以下内容必须原样展示在网页中，严禁修改、编造或替换）】\n");
        sb.append("姓名：").append(userName != null && !userName.isBlank() ? userName : "（未填写，显示占位即可）").append("\n");
        sb.append("口号：").append(slogan != null && !slogan.isBlank() ? slogan : "（未填写，该区块留空）").append("\n");
        sb.append("个人简介：").append(bio != null && !bio.isBlank() ? bio : "（未填写，该区块留空）").append("\n");
        sb.append("技能标签：").append(skills != null && !skills.isBlank() ? skills : "（未填写，该区块留空）").append("\n");
        sb.append("主题色（必须原样使用此色值）：").append(themeColor != null && !themeColor.isBlank() ? themeColor : "#4F46E5").append("\n");

        sb.append("\n作品项目：\n");
        if (projects != null && !projects.isEmpty()) {
            for (int i = 0; i < projects.size(); i++) {
                Map<String, String> p = projects.get(i);
                String title = p.getOrDefault("title", "");
                String desc = p.getOrDefault("description", "");
                if (title.isBlank() && desc.isBlank()) continue;
                sb.append(i + 1).append(". 标题：").append(title.isBlank() ? "（无标题）" : title);
                sb.append(" | 描述：").append(desc.isBlank() ? "（无描述）" : desc).append("\n");
            }
        } else {
            sb.append("（用户未提供作品项目，不要编造项目，留出作品区块占位即可）\n");
        }

        sb.append("\n请严格按照以上用户信息生成网页，文字内容必须与上方完全一致。\n");
        return sb.toString();
    }

    private String buildRagContext(List<StylePrompt> styles) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据用户信息检索到以下最相关的设计风格：\n\n");
        for (int i = 0; i < styles.size(); i++) {
            StylePrompt s = styles.get(i);
            sb.append("### 风格 ").append(i + 1).append("：").append(s.getName()).append("\n");
            sb.append("描述：").append(s.getDescription()).append("\n");
            sb.append("提示词：").append(s.getPromptTemplate()).append("\n\n");
        }
        sb.append("请综合以上风格的视觉特征，融合生成一个独特的个人门户设计。\n");
        return sb.toString();
    }

    private String buildUserPromptContext(String userPrompt, List<StylePrompt> retrieved) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户自定义设计要求：\n").append(userPrompt).append("\n\n");
        sb.append("参考风格提示词（用于补充设计细节）：\n");
        for (StylePrompt s : retrieved) {
            sb.append("- ").append(s.getName()).append("：").append(s.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 轻量 DeepSeek 调用：用于短文本任务（如关键词提取），不需要 json_object 格式。
     */
    private String callDeepSeek(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "max_tokens", 200,
                "stream", false
        );

        String response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    private String callLlm(String systemPrompt, String userPrompt, boolean isRetry) throws Exception {
        String retryHint = isRetry ? "\n\n【重要】上次输出不合规被丢弃。请严格做到：第一个字符是 '<'，以 <!DOCTYPE html> 开头，以 </html> 结尾，绝对不要 markdown 代码块标记，绝对不要任何说明文字。" : "";

        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt + retryHint),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.4,
                "max_tokens", 8192,
                "stream", false
        );

        String response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        // 检查是否被截断
        JsonNode finishNode = root.path("choices").path(0).path("finish_reason");
        String finishReason = finishNode.asText("");
        if ("length".equals(finishReason)) {
            log.warn("LLM 输出可能被 max_tokens 截断（finish_reason=length）");
        }
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    /** 从 LLM 返回中提取 HTML（可能包裹在 JSON、markdown 代码块或前后缀说明文字中） */
    private String extractHtml(String content) {
        if (content == null || content.isBlank()) return "";

        // 1) 尝试 JSON 解析（response_format=json_object 时 LLM 可能返回 {"html":"..."})
        try {
            JsonNode node = objectMapper.readTree(content);
            if (node.has("html")) {
                return node.get("html").asText();
            }
            for (JsonNode val : node) {
                if (val.isTextual() && val.asText().toLowerCase().contains("<!doctype")) {
                    return val.asText();
                }
            }
        } catch (Exception ignored) {
            // 不是 JSON，按纯文本处理
        }

        String text = content.trim();

        // 2) 用正则提取 ```html ... ``` 或 ``` ... ``` 代码块（DOTALL 跨行匹配）
        java.util.regex.Pattern codeBlock = java.util.regex.Pattern.compile(
                "```(?:html|HTML)?\\s*([\\s\\S]*?)```");
        java.util.regex.Matcher m = codeBlock.matcher(text);
        if (m.find()) {
            String inner = m.group(1).trim();
            if (inner.toLowerCase().contains("<!doctype") || inner.toLowerCase().contains("<html")) {
                return sliceHtml(inner);
            }
        }

        // 3) 兜底：直接截取 <!DOCTYPE ... </html>（处理前后带说明文字的情况）
        return sliceHtml(text);
    }

    /** 截取从 <!DOCTYPE 到 </html> 的内容（大小写不敏感，处理被截断的情况） */
    private String sliceHtml(String text) {
        String lower = text.toLowerCase();
        int start = lower.indexOf("<!doctype");
        if (start < 0) start = lower.indexOf("<html");
        if (start < 0) return text.trim();
        int end = lower.lastIndexOf("</html>");
        if (end < 0) end = text.length();  // 被截断时取到末尾
        else end = end + "</html>".length();
        return text.substring(start, end).trim();
    }

    /** 校验 HTML 是否符合门户网页要求 */
    private boolean validateHtml(String html) {
        if (html == null || html.isBlank()) return false;
        String lower = html.toLowerCase();
        return lower.contains("<!doctype html")
                && lower.contains("<html")
                && lower.contains("</html>")
                && lower.contains("<body")
                && lower.contains("</body>")
                && (lower.contains("<style") || lower.contains("style="))
                && !lower.contains("```");
    }

    /** 降级 HTML（LLM 失败时用简单模板） */
    private String buildFallbackHtml(String userName, String bio, String skills, String slogan,
                                     String themeColor, List<Map<String, String>> projects) {
        String color = themeColor != null ? themeColor : "#4F46E5";
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        sb.append("<title>").append(userName != null ? userName : "个人作品集").append("</title>");
        sb.append("<style>");
        sb.append("*{margin:0;padding:0;box-sizing:border-box}");
        sb.append("body{font-family:system-ui,sans-serif;background:#fafafa;color:#1a1714;padding:60px 20px;text-align:center}");
        sb.append(".container{max-width:800px;margin:0 auto}");
        sb.append("h1{font-size:36px;margin-bottom:8px;color:").append(color).append("}");
        sb.append(".slogan{display:inline-block;padding:4px 16px;border-radius:999px;background:").append(color).append(";color:#fff;font-size:14px;margin:8px 0}");
        sb.append(".bio{color:#666;max-width:500px;margin:16px auto;line-height:1.8}");
        sb.append(".skills{margin:16px 0}");
        sb.append(".skill{display:inline-block;padding:4px 12px;border:1px solid ").append(color).append(";border-radius:999px;margin:4px;font-size:13px;color:").append(color).append("}");
        sb.append(".projects{margin-top:32px;text-align:left}");
        sb.append(".project{background:#fff;border-radius:12px;padding:20px;margin-bottom:12px;box-shadow:0 1px 3px rgba(0,0,0,.08)}");
        sb.append(".project h3{font-size:18px;margin-bottom:8px}");
        sb.append(".project p{color:#666;font-size:14px}");
        sb.append("</style></head><body><div class=\"container\">");
        sb.append("<h1>").append(userName != null ? userName : "").append("</h1>");
        if (slogan != null && !slogan.isBlank()) {
            sb.append("<span class=\"slogan\">").append(slogan).append("</span>");
        }
        if (bio != null && !bio.isBlank()) {
            sb.append("<p class=\"bio\">").append(bio).append("</p>");
        }
        if (skills != null && !skills.isBlank()) {
            sb.append("<div class=\"skills\">");
            for (String s : skills.split(",")) {
                sb.append("<span class=\"skill\">").append(s.trim()).append("</span>");
            }
            sb.append("</div>");
        }
        if (projects != null && !projects.isEmpty()) {
            sb.append("<div class=\"projects\">");
            for (Map<String, String> p : projects) {
                sb.append("<div class=\"project\"><h3>").append(p.getOrDefault("title", "")).append("</h3>");
                sb.append("<p>").append(p.getOrDefault("description", "")).append("</p></div>");
            }
            sb.append("</div>");
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }
}
