package dev.blackholemax.backend.dto;

import java.util.List;

public record AiResult() {

    public record AiBio(
            String slogan,
            String story,
            List<String> skills
    ) {
    }

    public record AiDescription(
            String text
    ) {
    }

    public record AiColor(
            String color,
            String reason
    ) {
    }

    public record AiSeo(
            String title,
            String description
    ) {
    }

    public record AiPortalHtml(
            String html,
            String message,
            String imageKeywords
    ) {
    }
}