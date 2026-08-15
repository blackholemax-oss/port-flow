package dev.blackholemax.backend.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.blackholemax.backend.entity.Portfolio;
import dev.blackholemax.backend.entity.Project;

import java.util.List;

/**
 * 对外展示页的只读数据模型（缓存友好）：
 * 从 JPA 实体脱敏转换而来，避免 Hibernate PersistentBag/代理对象进入 Redis 序列化。
 * 注意：record 是 final 类型，NON_FINAL 默认类型化不覆盖，需显式标注 @JsonTypeInfo 才能在 Redis 中携带类型信息。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record PortfolioPageData(
        String slug,
        String userName,
        String slogan,
        String bio,
        String skills,
        String themeColor,
        String template,
        String seoTitle,
        String seoDescription,
        String avatarPath,
        String generatedHtml,
        List<ProjectData> projects
) {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public record ProjectData(String title, String description, String coverPath) {
        static ProjectData from(Project project) {
            return new ProjectData(project.getTitle(), project.getDescription(), project.getCoverPath());
        }
    }

    public static PortfolioPageData from(Portfolio portfolio) {
        List<ProjectData> projects = portfolio.getProjects() == null
                ? List.of()
                : portfolio.getProjects().stream().map(ProjectData::from).toList();
        return new PortfolioPageData(
                portfolio.getSlug(),
                portfolio.getUserName(),
                portfolio.getSlogan(),
                portfolio.getBio(),
                portfolio.getSkills(),
                portfolio.getThemeColor(),
                portfolio.getTemplate(),
                portfolio.getSeoTitle(),
                portfolio.getSeoDescription(),
                portfolio.getAvatarPath(),
                portfolio.getGeneratedHtml(),
                projects
        );
    }
}