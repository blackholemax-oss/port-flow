package dev.blackholemax.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 仅当 ai.deepseek.api-key 配置了非空值时启用真实 LLM 实现。
 */
public class DeepSeekEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String apiKey = context.getEnvironment().getProperty("ai.deepseek.api-key");
        return StringUtils.hasText(apiKey);
    }
}
