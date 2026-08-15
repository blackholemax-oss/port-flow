package dev.blackholemax.backend.dto;

import java.util.List;

public record PortfolioFormDTO(
        String slug,
        String userName,
        String slogan,
        String bio,
        String skills,
        String themeColor,
        String template,
        boolean isPublished,
        String avatarPath,
        String seoTitle,
        String seoDescription,
        String generatedHtml,
        String customPrompt,
        List<String> projectTitles,
        List<String> projectDescriptions,
        List<String> projectCovers
) {
}
