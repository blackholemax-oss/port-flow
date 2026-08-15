package dev.blackholemax.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project", indexes = {
        @Index(name = "idx_project_portfolio", columnList = "portfolio_id")
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 255)
    private String coverPath;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 外键（只读视图，由 {@link #getPortfolio()} 关联驱动写入）。
     */
    public Long getPortfolioId() {
        return portfolio != null ? portfolio.getId() : null;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }
}
