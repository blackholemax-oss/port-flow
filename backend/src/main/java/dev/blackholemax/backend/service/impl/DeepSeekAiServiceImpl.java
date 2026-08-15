package dev.blackholemax.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blackholemax.backend.config.DeepSeekEnabledCondition;
import dev.blackholemax.backend.dto.AiResult;
import dev.blackholemax.backend.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大模型实现（OpenAI 兼容协议）：配置 ai.deepseek.api-key 后自动启用。
 * 任何异常（超时/报错/解析失败）自动降级为 Mock 规则实现，不阻断编辑流程。
 */
@Service
@Primary
@Conditional(DeepSeekEnabledCondition.class)
public class DeepSeekAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiServiceImpl.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MockAiServiceImpl fallback;
    private final String model;

    public DeepSeekAiServiceImpl(
            @Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl,
            @Value("${ai.deepseek.model}") String model,
            @Value("${ai.deepseek.timeout-seconds:30}") int timeoutSeconds,
            ObjectMapper objectMapper,
            MockAiServiceImpl fallback) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    private String call(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.7
        );
        String response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        return content;
    }

    private JsonNode parseJson(String content) throws Exception {
        return objectMapper.readTree(content);
    }

    @Override
    public AiResult.AiBio generateBio(String keywords) {
        try {
            String content = call(
                    """
                    你是资深的个人品牌策展师和创意文案专家。用户会给你一些灵感标签（零散的关键词、短语或句子），
                    你需要从中洞察这个人的特质、职业方向和个性魅力，然后生成有温度、有记忆点的个人品牌内容。

                    【生成要求】
                    1. slogan：一句朗朗上口的个人口号（8-16 字），要有节奏感和画面感
                       - 好：「用代码雕刻光的形状」「把混乱变有序，把复杂变简单」
                       - 差：「追求卓越」「热爱技术」「不断前行」（太空泛）
                    2. story：100-180 字的个人故事，用第一人称口吻，像跟朋友聊天一样自然
                       - 必须融入灵感标签中的具体元素，不要空谈
                       - 可以有一个"转折"或"顿悟" moment 让故事有记忆点
                       - 避免套话：「我始终坚持不懈」「我相信细节决定成败」
                    3. skills：3-6 个技能标签，从灵感标签中提炼专业能力
                       - 用行业通用术语（如 "React" 而非 "前端框架"，"分布式系统" 而非 "后端"）
                       - 如果标签偏抽象，推测最可能的专业技能

                    【创意原则】
                    - 从标签中找到独特的"人设钩子"，让内容有辨识度
                    - 如果标签比较抽象（如"咖啡""独立开发"），找到它们之间的内在联系
                    - 如果标签偏技术，文案也要有温度；如果标签偏文艺，也要体现专业感
                    - 严禁编造具体数字、公司名、项目名等虚构事实

                    只输出 JSON，不要任何其他文字。
                    """,
                    "灵感标签：" + keywords + "\n\n输出格式：{\"slogan\":\"一句话口号\",\"story\":\"100-180字的个人故事\",\"skills\":[\"技能1\",\"技能2\",\"技能3\"]}"
            );
            JsonNode node = parseJson(content);
            List<String> skills = new ArrayList<>();
            node.path("skills").forEach(s -> skills.add(s.asText()));
            return new AiResult.AiBio(
                    node.path("slogan").asText(),
                    node.path("story").asText(),
                    skills
            );
        } catch (Exception e) {
            log.warn("AI 生成 Bio 失败，降级为 Mock 实现：{}", e.getMessage());
            return fallback.generateBio(keywords);
        }
    }

    @Override
    public AiResult.AiDescription polishDescription(String raw, String title) {
        try {
            String userPrompt = "原始描述：" + raw;
            if (title != null && !title.isBlank()) {
                userPrompt = "作品标题：" + title + "\n" + userPrompt;
            }
            userPrompt += "\n\n输出格式：{\"text\":\"润色后的描述\"}";
            String content = call(
                    """
                    你是资深技术作品集文案润色师。你的任务是把用户口语化的作品描述润色为专业、精炼、有吸引力的作品介绍。

                    【润色原则】
                    1. 保留原文的核心信息，不编造虚假数据或功能
                    2. 语言精炼专业，去除口语化表达（如"就是""然后"" basically"）
                    3. 突出技术亮点和实际价值，让读者快速理解项目做了什么、解决了什么问题
                    4. 控制在 50-120 字，一句话说清项目定位，再补充 1-2 句技术亮点
                    5. 如果提供了作品标题，结合标题理解项目背景，但润色结果只输出描述部分（不含标题）

                    【润色示例】
                    - 原文：「做了一个电商网站，用 React 写的，主要是卖东西的」
                      润色：「基于 React + Node.js 的全栈电商平台，支持商品管理、订单流转与支付对接，覆盖完整电商链路。」
                    - 原文：「写了个工具自动部署，用 Python 脚本，省了很多手动操作」
                      润色：「Python 自动化部署工具，一键完成多环境配置与发布，将部署耗时从 30 分钟缩短至 2 分钟。」

                    【禁止】
                    - 严禁编造虚构的数据指标（如"日均 10W+ 请求"），除非原文提及
                    - 严禁添加原文没有的技术栈或功能
                    - 严禁使用"业界领先""革命性"等夸张表述

                    只输出 JSON，不要任何其他文字。
                    """,
                    userPrompt
            );
            return new AiResult.AiDescription(parseJson(content).path("text").asText());
        } catch (Exception e) {
            log.warn("AI 润色描述失败，降级为 Mock 实现：{}", e.getMessage());
            return fallback.polishDescription(raw, title);
        }
    }

    @Override
    public AiResult.AiColor recommendColor(String occupation) {
        try {
            String content = call(
                    "你是资深品牌视觉顾问。根据用户的职业属性推荐一个适合作品集的主题色，输出十六进制色值并附上推荐理由，输出 JSON。",
                    "职业：" + occupation + "\n输出格式：{\"color\":\"#RRGGBB\",\"reason\":\"推荐理由\"}"
            );
            JsonNode node = parseJson(content);
            return new AiResult.AiColor(node.path("color").asText(), node.path("reason").asText());
        } catch (Exception e) {
            log.warn("AI 推荐配色失败，降级为 Mock 实现：{}", e.getMessage());
            return fallback.recommendColor(occupation);
        }
    }

    @Override
    public AiResult.AiSeo generateSeo(String userName, String bio) {
        try {
            String content = call(
                    "你是 SEO 专家。根据用户的姓名和简介生成作品集页面的 Title 与 Meta Description，符合搜索引擎收录规范，输出 JSON。",
                    "姓名：" + userName + "，简介：" + bio + "\n输出格式：{\"title\":\"不超过30字的标题\",\"description\":\"不超过150字的描述\"}"
            );
            JsonNode node = parseJson(content);
            return new AiResult.AiSeo(node.path("title").asText(), node.path("description").asText());
        } catch (Exception e) {
            log.warn("AI 生成 SEO 失败，降级为 Mock 实现：{}", e.getMessage());
            return fallback.generateSeo(userName, bio);
        }
    }
}
