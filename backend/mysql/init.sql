-- ============================================================
-- AI 策展师 · 智能作品集生成器 - 数据库初始化脚本 (MySQL 8)
-- 使用方式：
--   1. docker-compose 首次启动时自动执行（挂载到
--      /docker-entrypoint-initdb.d/init.sql，仅空数据卷首次初始化时运行）
--   2. 手动执行：mysql -u root -p < init.sql
-- 注意：应用使用 spring.jpa.hibernate.ddl-auto=update，
--       本脚本与实体类生成的表结构保持一致，重复执行不会报错。
-- ============================================================

CREATE DATABASE IF NOT EXISTS portfolio
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE portfolio;

-- ------------------------------------------------------------
-- 用户表（对应实体 User，@Table app_user）
-- admin 字段：是否管理员（1=管理员，仅管理员可登录管理后台 admin-web）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(128) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    display_name  VARCHAR(64),
    admin         BIT(1)       NOT NULL DEFAULT b'0',
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 作品集表（对应实体 Portfolio，@Table portfolio）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS portfolio (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    slug            VARCHAR(64)  NOT NULL,
    user_id         BIGINT       NOT NULL,
    user_name       VARCHAR(64)  NOT NULL,
    slogan          VARCHAR(200),
    bio             VARCHAR(2000),
    skills          VARCHAR(500),
    theme_color     VARCHAR(16),
    template        VARCHAR(32),
    seo_title       VARCHAR(200),
    seo_description VARCHAR(500),
    avatar_path     VARCHAR(255),
    is_published    BIT(1)       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_slug (slug),
    KEY idx_portfolio_user (user_id),
    CONSTRAINT fk_portfolio_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 项目表（对应实体 Project，@Table project）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_id BIGINT       NOT NULL,
    title        VARCHAR(128) NOT NULL,
    description  VARCHAR(2000),
    cover_path   VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_project_portfolio (portfolio_id),
    CONSTRAINT fk_project_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 访问记录表（对应实体 VisitRecord，@Table visit_record）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS visit_record (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    portfolio_id BIGINT      NOT NULL,
    visitor_id   VARCHAR(64) NOT NULL,
    visited_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_visit_portfolio (portfolio_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- 演示数据（与 DataSeeder 保持一致，登录：demo@example.com / demo123）
-- ============================================================

INSERT INTO app_user (id, email, password_hash, display_name, created_at)
VALUES (1, 'demo@example.com', '$2a$10$o9SkNoxWo6gp0lTpyMVVG.a4vMhGs4F79nxcRW/P/KK8OqMerVblK', 'Demo User', NOW(6));

-- 管理员账号（admin@portflow.dev / admin123456，与 .env 中 ADMIN_EMAIL/ADMIN_PASSWORD 一致；
-- 启动时 AdminBootstrapConfig 会按环境变量再次校验/重置密码）
INSERT INTO app_user (email, password_hash, display_name, admin, created_at)
VALUES ('admin@portflow.dev', '$2b$10$9u69a7LH8s6J6wryVZ9.h.TSENYYX/qbOB4eikX6lhSdBGtdmM8VS', '管理员', b'1', NOW(6));

INSERT INTO portfolio (id, slug, user_id, user_name, slogan, bio, skills,
                       theme_color, template, seo_title, seo_description, avatar_path, is_published)
VALUES (1, 'demo', 1, 'Demo User', '让技术，成为我的名片',
        '热爱技术的独立开发者，专注于 Web 应用与创意编程，用作品说话。',
        'Java, Spring Boot, AI, 前端', '#4F46E5', 'card',
        'Demo User - 个人作品集 | AI 策展师',
        '热爱技术的独立开发者，展示 Web 应用与创意编程作品。',
        '/demo-avatar.svg', b'1');

INSERT INTO project (portfolio_id, title, description, cover_path)
VALUES (1, '作品集生成器', '输入个人资料即可生成一个极简风格的作品集页面，本项目就是用它做的。', '/demo-cover.svg'),
       (1, '极简时钟', '一个用 Canvas 手写的极简时钟，支持暗色模式。', '/demo-cover.svg'),
       (1, '数据可视化面板', '实时渲染指标数据的仪表盘，基于 WebSocket 推送。', '/demo-cover.svg');