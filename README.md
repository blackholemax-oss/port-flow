# PortFlow · AI 策展师

一个「零文案，一键生成个人门户」的智能作品集平台。用户只需填写一句灵感便签，AI 自动润色个人简介、推荐配色、生成 SEO，并基于个人信息与风格偏好生成完整 HTML 个人网页；同时支持联网图片搜索（image-crawler → Pexels → LoremFlickr 多级降级），为门户自动匹配与内容相关的配图。

## 功能特性

- **AI 一键生成门户**：选择「AI 定制门户」模板，AI 依据姓名、口号、个人故事、技能、作品项目生成完整 HTML 页面（DeepSeek 大模型，未配置 Key 时自动降级为 Mock 规则实现）
- **智能图片搜索**：根据用户输入提取关键词（作品名优先），仅在用户要求应用图片时触发；爬取结果通过 image-crawler → 内置百度 acjson → Pexels → LoremFlickr 多级降级
- **AI 魔法生成**：灵感便签生成个人简介、职业属性微调、配色推荐、SEO 标题/描述
- **丰富模板**：极简卡片 / 画廊大图 / 杂志风 三套内置模板 + AI 定制门户
- **网页实时调整**：生成后可通过对话式指令让 AI 调整页面，修改内容一键同步表单
- **作品集管理**：新建 / 编辑 / 发布 / 删除，独立对外展示地址 `/p/{slug}`
- **访问统计**：PV / UV 统计，仪表盘可视化
- **安全加固**：Sa-Token JWT 鉴权、BCrypt 密码加密、XSS 过滤、AI 生成 HTML 沙箱渲染

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Next.js 14 (App Router) · React 18 · TypeScript · Tailwind CSS · shadcn/ui · sonner |
| 后端 | Spring Boot 3.2 · Spring Web MVC · Spring Data JPA · Thymeleaf · Sa-Token + JWT · Caffeine + Redis 二级缓存 |
| 数据库 | MySQL 8（生产/本地默认）· H2（dev profile，内存库无需外部依赖） |
| AI | DeepSeek Chat API（`ai.deepseek.api-key` 未配置时自动使用 MockAiServiceImpl） |
| 图片爬取 | Python 3 + Flask（image-crawler HTTP API 服务，:8120）· Pexels API · LoremFlickr |
| 构建 | Maven (wrapper) · Node.js / npm |
| 部署 | Docker Compose（MySQL + Redis + Backend） |

## 项目结构

```
port-flow/
├── backend/                  # Spring Boot 后端（:8080）
│   ├── src/main/java/...     # 控制器 / 服务 / 实体 / 配置
│   ├── src/main/resources/   # application.yml + 内置模板页（Thymeleaf）
│   ├── mysql/init.sql        # MySQL 建表 + 演示数据初始化脚本
│   ├── Dockerfile
│   └── docker-compose.yml    # MySQL + Redis + backend 一键编排
├── frontend/                 # Next.js 前端（:3000）
│   └── src/app/              # 登录/注册、仪表盘、编辑器、对外展示页
├── image-crawler/            # Python 图片爬虫 HTTP API 服务（:8120，后端通过 REST 调用）
├── uploads/                  # 运行时图片下载/上传目录（已 gitignore）
└── .env / .env.example       # 环境变量（敏感信息不提交）
```

## 环境要求

- JDK 17+
- Maven 3.8+（或使用内置 `mvnw`）
- Node.js 18+ / npm 9+
- MySQL 8 + Redis 7（本地开发默认 profile 需要；dev profile 仅需 Redis）
- Python 3.8+ 与 `requests`、`flask`（image-crawler 图片爬虫 HTTP API 服务）
- 可选：DeepSeek API Key（配置后启用真实大模型）、Pexels API Key（可选降级搜索）

## 快速开始

### 1. 配置环境变量

复制 `.env.example` 为 `.env` 并填写真实值：

```bash
cp .env.example .env
```

```dotenv
# 必填：DeepSeek AI Key（未配置则使用 Mock AI）
AI_DEEPSEEK_API_KEY=sk-你的key

# 必填：sa-token JWT 签名密钥（生产环境务必设置为随机值）
SA_TOKEN_SECRET=你的随机密钥

# 可选：Pexels 图片搜索 Key
PEXELS_API_KEY=

# MySQL / Redis（与 docker-compose 默认一致）
DB_HOST=localhost
DB_PORT=3306
DB_NAME=portfolio
DB_USER=root
DB_PASSWORD=root
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# image-crawler 图片爬虫 HTTP API 服务
# 启动方式：cd image-crawler && python server.py --port 8120
IMAGE_CRAWLER_API_URL=http://127.0.0.1:8120
```

> `.env` 已被 `.gitignore` 排除，请勿提交真实密钥。

### 2. 启动基础设施（MySQL + Redis）

```bash
cd backend
docker compose up -d mysql redis
```

> 不依赖 Docker 时，可自行安装 MySQL 8 + Redis，并确保 root/root 或通过 `.env` 中的 `DB_*` / `REDIS_*` 覆盖连接信息。数据库表结构由 `spring.jpa.hibernate.ddl-auto=update` 自动创建，也可手动执行 `mysql -u root -p < mysql/init.sql`。

### 3. 启动后端（Spring Boot，:8080）

Windows PowerShell（从 `.env` 加载环境变量后启动）：

```powershell
cd backend
Get-Content "..\.env" | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*\s*=' -and -not $_.TrimStart().StartsWith('#') } | ForEach-Object { $kv = $_ -split '=', 2; $k = $kv[0].Trim(); $v = $kv[1].Trim().Trim('"').Trim("'"); [Environment]::SetEnvironmentVariable($k, $v, "Process") }
.\mvnw.cmd spring-boot:run
```

Linux / macOS：

```bash
cd backend
export $(grep -v '^#' ../.env | xargs)
./mvnw spring-boot:run
```

启动成功标志：日志输出 `Tomcat started on port 8080`、`Started BackendApplication`。

> **仅做后端功能开发、不想装 MySQL**：使用 dev profile（H2 内存库），只需 Redis：
> `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`

### 4. 启动 image-crawler 图片爬虫 API 服务（:8120）

```bash
cd image-crawler
pip install -r requirements.txt
python server.py --port 8120
```

> 后端通过 HTTP 调用 `GET /api/search?keyword=<关键词>&count=<数量>` 获取图片 URL（可用 `http://127.0.0.1:8120/health` 验证服务存活）。若未启动该服务或未配置 `IMAGE_CRAWLER_API_URL`，后端会自动降级到内置百度 acjson → Pexels → LoremFlickr。

### 5. 启动前端（Next.js，:3000）

```bash
cd frontend
npm install
npm run dev
```

前端通过 `next.config.mjs` 将 `/api/*`、`/uploads/*`、`/demo/*` 同源代理到后端（默认 `http://127.0.0.1:8080`，可用环境变量 `BACKEND_URL` 覆盖）。

访问 http://localhost:3000 ，使用演示账号登录：`demo@example.com / demo123`。

## Docker 一键部署（生产）

```bash
cd backend
# 可选：在 .env 中设置 AI_DEEPSEEK_API_KEY=sk-xxx
docker compose up -d --build
```

将启动 3 个容器：`portfolio-mysql`（:3306，首次自动执行 init.sql）、`portfolio-redis`（:6379）、`portfolio-backend`（:8080）。前端需另行构建部署（`npm run build && npm start`），并通过 `BACKEND_URL` 指向后端。

> Docker 镜像基于 `eclipse-temurin:17`，默认不含 Python 爬虫服务；如需容器内启用 image-crawler 图片爬取，可在 docker-compose 中增加 image-crawler 服务（Python 镜像运行 `server.py`），并将 `IMAGE_CRAWLER_API_URL` 指向该服务（如 `http://image-crawler:8120`）。

## 环境变量总览

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `AI_DEEPSEEK_API_KEY` | 否 | DeepSeek API Key，未配置自动降级 Mock AI |
| `SA_TOKEN_SECRET` | 是 | sa-token JWT 签名密钥 |
| `PEXELS_API_KEY` | 否 | Pexels 图片搜索 Key |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | 否 | MySQL 连接（默认 localhost / 3306 / portfolio / root / root） |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 否 | Redis 连接（默认 localhost / 6379 / 空） |
| `IMAGE_CRAWLER_API_URL` | 否 | image-crawler Python API 服务地址（默认 `http://127.0.0.1:8120`） |
| `BACKEND_URL` | 否 | 前端代理的后端地址（默认 `http://127.0.0.1:8080`） |
| `SPRING_PROFILES_ACTIVE` | 否 | Spring profile：`dev`（H2）或 `docker` |

## 核心流程说明

### 图片搜索降级链

```
用户明确要求使用图片(needImages=true)
  → DeepSeek 提取关键词（primary=作品名, secondary=视觉风格）
  → image-crawler HTTP API 服务（过滤广告/小图，优先 objURL）
  → 失败 → 内置百度 acjson 接口
  → 失败 → Pexels API
  → 失败 → LoremFlickr
```

- 搜索关键词会透明返回前端（生成完成后 Toast 提示，如「已用关键词『动漫美少女插画 / 粉色 紫色 浪漫 梦幻』搜索 4 张图片」）
- 下载图片会校验文件头魔数（JPEG/PNG/WebP/GIF）与最小体积，避免无效文件入库
- AI 生成 HTML 中背景图遮罩透明度 ≤ 0.4，保证图片可见

### 网页生成与调整

- 「AI 定制门户」生成完整 HTML，以 iframe sandbox（`allow-scripts`）渲染，防止脚本越权
- 修改左侧表单后需点击「📋 同步表单到网页」使网页与表单数据一致
- 生成后可通过对话指令让 AI 调整网页，调整结果自动保存

## 演示账号

| 账号 | 密码 | 用途 |
| --- | --- | --- |
| `demo@example.com` | `demo123` | 后台登录演示账号（由 DataSeeder 初始化） |

## 测试

```bash
cd backend
.\mvnw.cmd test        # 单元测试（H2 内存库）
```

## 文档

- [E2E 测试报告](docs/E2E-测试报告.md)
- [Web 端原型图](design/PortFlow-作品集管理平台-Web端原型图.pdf)
