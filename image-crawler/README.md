# image-crawler 图片爬虫

用于从百度图片搜索结果中爬取图片的 Python 工具，以 **HTTP API 服务**方式对外提供能力（本项目后端通过 REST 调用），也保留命令行方式。

## HTTP API 服务（推荐，供 Java 后端调用）

```bash
pip install -r requirements.txt
python server.py --host 0.0.0.0 --port 8120
```

| 接口 | 说明 |
| --- | --- |
| `GET /health` | 健康检查，返回 `{"status": "ok", "service": "image-crawler"}` |
| `GET /api/search?keyword=<关键词>&count=<数量>` | 按关键词搜索图片 URL，返回 `{"keyword": ..., "count": n, "urls": [...]}` |

示例：

```bash
curl "http://127.0.0.1:8120/api/search?keyword=动漫美少女插画&count=4"
```

> Java 后端通过 `WebImageSearchService` 调用该服务；未启动或未配置 `IMAGE_CRAWLER_API_URL` 时自动降级到内置百度 acjson → Pexels → LoremFlickr。

## 命令行使用（可选）

```bash
python main.py search "动漫美少女" -n 4    # 搜索图片 URL
python main.py download "动漫美少女" -n 4  # 搜索并下载到 Pictures/
```

## 原有功能（上游参考，当前版本未启用 GUI）

### 方法一：图形界面使用（推荐）

```bash
python gui.py
```

启动图形界面后，您可以：
- 输入搜索关键词
- 设置爬取页数和请求间隔
- 选择是否启用GSM反爬虫绕过
- 实时查看运行日志
- 监控爬取进度和状态

### 方法二：命令行直接使用

```bash
python main.py "疯狂动物城2" --pages 2 --delay 2 --gsm True
```

### 方法三：作为模块导入使用

```python
from image_crawler import crawl_baidu_images

crawl_baidu_images("疯狂动物城2", page_count=2)
```

### 方法四：使用原始接口

```python
from image_crawler import climb_image

climb_image("疯狂动物城2", 2)
```

## 参数说明

### 命令行参数

```bash
python main.py <keyword> [-p PAGES] [-d DELAY]
```

- `keyword`: 搜索关键词（必填）
- `-p, --pages`: 爬取页数，默认为1
- `-d, --delay`: 请求间隔时间(秒)，默认为1秒

### crawl_baidu_images(keyword, page_count=1, delay=1)

- `keyword`: 搜索关键词
- `page_count`: 爬取页数，默认为1
- `delay`: 请求间隔时间(秒)，默认为1秒

## 常见问题及解决方案

### 1. 爬取失败问题

原代码经常爬取失败的原因：

1. 请求头信息不完整，容易被识别为爬虫
2. 缺乏超时设置和异常处理
3. URL解码处理不当
4. 没有合适的延时机制，请求过于频繁

### 2. 改进措施

新的实现在以下方面进行了优化：

1. 添加了完整的浏览器请求头信息
2. 实现了完善的异常处理机制
3. 增加了超时设置
4. 添加了请求间隔，避免过于频繁访问
5. 改进了URL解析和解码逻辑
6. 日志输出
7. gsm绕反爬

## 项目结构

```txt
image-crawler/
├── image_crawler/
│   ├── __init__.py      # 包初始化文件
│   └── crawler.py       # 爬虫核心实现
├── main.py              # 命令行入口
├── gui.py               # 图形界面入口
├── requirements.txt     # 依赖包列表
└── Pictures/            # 图片自动保存目录
```

## 注意事项

1. 请合理控制爬取频率，避免对服务器造成压力
2. 遵守网站的robots.txt协议
3. 本工具仅供学习交流使用，请勿用于商业用途
4. 如遇到验证码或IP被封，建议增加延时或使用代理IP
5. 启用Selenium提取gsm需要确保安装了对应浏览器的webdriver，确保webdriver与浏览器版本一致，否则会失败，本项目使用chrome浏览器完成测试，可自行修改。
6. GUI界面基于Dear PyGui开发，首次运行时会自动下载相关依赖
7. 图形界面提供了更友好的操作体验，建议优先使用GUI版本

## 版权声明

本项目仅供学习交流使用，请尊重图片版权。
