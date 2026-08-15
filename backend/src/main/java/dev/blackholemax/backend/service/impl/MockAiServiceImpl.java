package dev.blackholemax.backend.service.impl;

import dev.blackholemax.backend.dto.AiResult;
import dev.blackholemax.backend.service.AiService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 规则模板实现的 AI 引擎：零外部依赖，未配置 LLM API Key 时使用，保证演示可跑通。
 * 不标注 @Primary：当 DeepSeek 实现启用时优先注入 DeepSeek，未启用时才用 Mock。
 */
@Service
public class MockAiServiceImpl implements AiService {

    private static final Map<String, String> COLOR_MAP = Map.of(
            "程序员", "#4F46E5",
            "设计师", "#EC4899",
            "摄影师", "#0EA5E9",
            "产品经理", "#F59E0B"
    );

    private static final Map<String, String> COLOR_REASON_MAP = Map.of(
            "程序员", "靛蓝色沉稳理性，契合技术人的严谨气质",
            "设计师", "品红色充满创造力，突出视觉敏感度",
            "摄影师", "天蓝色通透干净，呼应光影与镜头",
            "产品经理", "琥珀色温暖积极，传递协作与洞察"
    );

    private List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of("创造", "热爱", "成长");
        }
        return Arrays.stream(keywords.split("[,，、;；\\s]+"))
                .filter(k -> !k.isBlank())
                .limit(5)
                .toList();
    }

    @Override
    public AiResult.AiBio generateBio(String keywords) {
        List<String> kws = splitKeywords(keywords);
        String first = kws.get(0);
        String second = kws.size() > 1 ? kws.get(1) : "创新";

        String slogan = "让" + first + "，成为我的名片";
        String story = "我从「" + first + "」出发，在「" + second + "」上不断打磨细节。"
                + "我相信好的作品不是堆砌功能，而是把每个环节做到恰到好处，"
                + "让使用者感受到背后的用心与专业。";
        return new AiResult.AiBio(slogan, story, kws);
    }

    @Override
    public AiResult.AiDescription polishDescription(String raw, String title) {
        if (raw == null || raw.isBlank()) {
            return new AiResult.AiDescription("一段值得被记住的作品经历：从想法到落地，覆盖完整链路，持续打磨每一处体验。");
        }
        String text = "把「" + raw.trim() + "」从想法变为现实：面向真实用户打磨核心流程，"
                + "沉淀可复用的工程实践，支撑日均 10W+ 请求稳定运行，收获大量正向反馈。";
        return new AiResult.AiDescription(text);
    }

    @Override
    public AiResult.AiColor recommendColor(String occupation) {
        String key = occupation == null || occupation.isBlank() ? "程序员" : occupation.trim();
        String color = COLOR_MAP.getOrDefault(key, "#4F46E5");
        String reason = COLOR_REASON_MAP.getOrDefault(key, "经典靛蓝，百搭且富有专业感");
        return new AiResult.AiColor(color, reason);
    }

    @Override
    public AiResult.AiSeo generateSeo(String userName, String bio) {
        String name = userName == null || userName.isBlank() ? "我的" : userName.trim();
        String desc = bio == null || bio.isBlank()
                ? name + " 的个人作品集，用作品说话。"
                : bio.trim();
        if (desc.length() > 150) {
            desc = desc.substring(0, 150);
        }
        return new AiResult.AiSeo(name + " - 个人作品集 | AI 策展师", desc);
    }
}