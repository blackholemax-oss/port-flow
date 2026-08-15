package dev.blackholemax.backend.config;

import dev.blackholemax.backend.entity.StylePrompt;
import dev.blackholemax.backend.repository.StylePromptRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 设计风格提示词种子数据：从 stylekit.top 提取的 12 大风格分类。
 * 幂等：已存在则跳过。
 */
@Configuration
public class StylePromptSeeder {

    @Bean
    @Order(1) // 先于 PortfolioDataSeeder 执行
    public CommandLineRunner seedStyles(StylePromptRepository repo) {
        return args -> {
            // 已有数据时，修正历史风格名称（去除技术名词）
            if (repo.count() > 0) {
                repo.findAll().forEach(s -> {
                    if ("tailwind-ui".equals(s.getCode()) && !"现代组件库".equals(s.getName())) {
                        s.setName("现代组件库");
                        repo.save(s);
                    }
                    if ("corporate-saas".equals(s.getCode()) && !"企业商务".equals(s.getName())) {
                        s.setName("企业商务");
                        repo.save(s);
                    }
                });
                return;
            }

            repo.saveAll(List.of(
                new StylePrompt("minimalist", "极简设计",
                    "极简设计去除装饰性元素，让内容自己说话。依靠排版、留白和微妙的对比度，每一个设计决策都必须有意图。适合作家作品集、摄影作品集、个人博客、产品文档。",
                    "minimal minimalist clean simple whitespace 简洁 极简 留白 干净 简约 简单",
                    "Design a minimalist portfolio. Single-column layout, max-width 680px centered. System serif font for headings, sans-serif for body, line-height 1.8. Only black text on white, single accent color for links. No borders, no shadows, no icons. Let whitespace and typography do all the work.",
                    "作家作品集、摄影作品集、个人博客、产品文档"),

                new StylePrompt("cyberpunk", "赛博朋克",
                    "赛博朋克从科幻中汲取灵感，暗底霓虹配色、故障和扫描线效果、HUD元素、未来感排版和粗粝都市美学。适合游戏平台、音乐夜生活、科技产品发布、数据可视化。",
                    "cyberpunk neon futuristic sci-fi hud glitch dark neon 赛博 朋克 霓虹 未来 科技 科幻 暗",
                    "Dark base (#0a0a0f) with neon accents: cyan (#00f0ff), magenta (#ff0080). HUD-style corners on cards. Monospace font for all text. Glitch effect on headings, scanline overlay. Cards with angled/clipped corners using clip-path polygon.",
                    "游戏平台、音乐夜生活、科技产品发布、数据可视化"),

                new StylePrompt("glassmorphism", "玻璃拟态",
                    "玻璃拟态通过 backdrop-filter: blur()、半透明背景和微妙边框创造毛玻璃效果。层叠半透明表面创造深度感。适合音乐播放器、天气应用、Hero覆层、Apple风格应用。",
                    "glassmorphism glass blur frosted transparent backdrop glass 玻璃 模糊 半透明 毛玻璃",
                    "Glassmorphism cards over gradient background. background rgba(255,255,255,0.15), backdrop-filter blur(12px), border 1px solid rgba(255,255,255,0.2), border-radius 16px. Text in white with varying opacity for hierarchy.",
                    "音乐播放器、天气应用、Hero覆层、macOS/iOS风格应用"),

                new StylePrompt("neo-brutalist", "新野兽派",
                    "新野兽派拥抱原始、未经打磨的美学，大胆黑色粗边框、硬边缘阴影、零圆角、高对比色彩组合和刻意的不完美布局。适合创意机构、开发者作品集、独立产品、活动会议。",
                    "brutalist neo-brutalist bold raw anti-design thick-border hard-shadow 野兽 粗犷 大胆 原始",
                    "3px solid black borders, no border-radius, hard shadows (4px 4px 0px #000). Bold condensed sans-serif for headings, monospace for body. Intentionally asymmetric grid, overlapping elements, rotated cards. High contrast accent colors.",
                    "创意机构站点、开发者作品集、独立产品、活动会议"),

                new StylePrompt("dark-mode", "暗黑模式",
                    "暗黑模式设计优雅暗色主题界面，涵盖对比度、色彩层级和可读性。适合代码工具、开发者文档、媒体平台、夜间使用场景。",
                    "dark mode theme dark-theme dark-bg 暗色 暗黑 黑色 深色 夜间",
                    "Elegant dark theme. Background #0f0f0f, cards #1a1a1a, text #e0e0e0. Subtle borders rgba(255,255,255,0.1). Accent color for interactive elements. Good contrast ratios, comfortable reading in low light.",
                    "代码工具、开发者文档、媒体平台、夜间使用"),

                new StylePrompt("dashboard", "仪表盘",
                    "数据丰富的仪表盘UI，涵盖图表、指标卡片、侧边栏和响应式布局。适合数据分析、监控面板、管理后台、运营看板。",
                    "dashboard chart metrics analytics admin panel data 仪表 盘 数据 图表 监控 管理",
                    "Data-rich dashboard with chart cards, metric tiles, sidebar navigation. Grid layout with responsive breakpoints. Card-based components with shadows. Data tables, status badges, progress bars. Clean and information-dense.",
                    "数据分析平台、监控面板、管理后台、运营看板"),

                new StylePrompt("landing-page", "落地页",
                    "高转化落地页AI提示词，涵盖Hero区块、功能网格、用户评价和CTA。适合产品发布、SaaS营销、应用推广、活动报名。",
                    "landing page hero cta conversion marketing 落地 营销 转化 hero 产品",
                    "High-conversion landing page. Hero section with headline, subheadline, CTA button. Feature grid with icons. Social proof section. Clear visual hierarchy. Gradient or image background. Bold typography, generous spacing.",
                    "产品发布、SaaS营销、应用推广、活动报名"),

                new StylePrompt("retro-vintage", "复古设计",
                    "灵感来自Y2K、80年代霓虹、VHS、复古计算和怀旧美学。适合复古品牌、怀旧产品、音乐艺人、创意工作室。",
                    "retro vintage y2k 80s neon vhs nostalgic 复古 怀旧 80年代 经典",
                    "Retro aesthetic with Y2K/80s inspiration. Warm muted colors, pixelated textures, retro fonts. VHS scanline effects, neon glow accents. Nostalgic computing elements. Vintage color palette: amber, teal, cream, rust.",
                    "复古品牌、怀旧产品、音乐艺人、创意工作室"),

                new StylePrompt("anime-manga", "动漫风格",
                    "灵感来自动漫的网页界面，日式美学、鲜艳色彩和角色驱动设计。适合动漫社区、游戏、创意作品、日本文化。",
                    "anime manga japanese kawaii cute colorful 动漫 卡通 可爱 日式 二次元",
                    "Anime-inspired UI with vibrant colors, cute illustrations, rounded shapes. Pastel accent colors, playful animations. Japanese typography influences. Character-driven design elements. Kawaii aesthetic with soft shadows and rounded corners.",
                    "动漫社区、游戏、创意作品、日本文化"),

                new StylePrompt("corporate-saas", "企业商务",
                    "专业、可信赖的商业界面，简洁布局、数据表格和转化优化模式。适合企业官网、SaaS产品、B2B服务、金融科技。",
                    "corporate saas professional business enterprise b2b 企业 商务 专业 saas b2b",
                    "Professional B2B interface. Clean white background, blue accent. Data tables, breadcrumb navigation. Sidebar with collapsible sections. Trust-building elements: testimonials, logos, security badges. Conversion-optimized CTAs.",
                    "企业官网、SaaS产品、B2B服务、金融科技"),

                new StylePrompt("japanese-aesthetic", "日式美学",
                    "灵感来自禅意、侘寂和传统日式美学，自然材质和用心间距。适合茶道、和风品牌、冥想应用、文化展示。",
                    "japanese zen wabi-sabi minimalist natural aesthetic 日式 禅 侘寂 和风 自然",
                    "Japanese zen aesthetic. Natural materials: wood, paper, stone textures. Muted earth tones: beige, sage, slate. Generous negative space (ma). Vertical typography accents. Subtle animations. Wabi-sabi imperfection celebrated.",
                    "茶道文化、和风品牌、冥想应用、文化展示"),

                new StylePrompt("tailwind-ui", "现代组件库",
                    "为Tailwind CSS优化的AI提示词，响应式布局、组件模式、design tokens和工具类优先样式。适合快速原型、组件库、设计系统、技术文档。",
                    "tailwind css utility-first responsive components design-tokens tailwind 响应式 组件 工具类",
                    "Tailwind CSS utility-first design. Responsive grid, consistent spacing scale. Component patterns: cards, badges, buttons, inputs. Design tokens via Tailwind config. Modern, clean, highly customizable. Mobile-first breakpoints.",
                    "快速原型、组件库、设计系统、技术文档")
            ));
        };
    }
}
