package dev.blackholemax.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 网络图片搜索服务：
 * 降级策略：
 * 1. 百度图片搜索（免认证、支持中文关键词、图片量大）
 * 2. Pexels API（需 API key，高质量）
 * 3. LoremFlickr（免认证，最终兜底）
 * 搜索到的图片下载到本地 uploads/ 目录，返回可访问的相对路径。
 */
@Service
public class WebImageSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebImageSearchService.class);

    private final RestClient pexelsClient;
    private final RestClient baiduClient;
    private final RestClient crawlerClient;
    private final ObjectMapper objectMapper;
    private final boolean pexelsEnabled;
    private final String crawlerApiUrl;

    public WebImageSearchService(
            @Value("${pexels.api-key:}") String apiKey,
            @Value("${image.crawler-api-url:}") String crawlerApiUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.pexelsEnabled = apiKey != null && !apiKey.isBlank();
        this.crawlerApiUrl = crawlerApiUrl;
        // image-crawler Python HTTP API 服务
        this.crawlerClient = (crawlerApiUrl != null && !crawlerApiUrl.isBlank())
                ? RestClient.builder().baseUrl(crawlerApiUrl).build()
                : null;
        if (pexelsEnabled) {
            this.pexelsClient = RestClient.builder()
                    .baseUrl("https://api.pexels.com/v1")
                    .defaultHeader("Authorization", apiKey)
                    .build();
            log.info("WebImageSearchService 已启用（image-crawler API > Pexels > LoremFlickr）");
        } else {
            this.pexelsClient = null;
            log.info("WebImageSearchService 已启用（image-crawler API > LoremFlickr，配置 pexels.api-key 可启用 Pexels 降级）");
        }
        // 百度客户端：模拟浏览器请求
        this.baiduClient = RestClient.builder()
                .baseUrl("https://image.baidu.com")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json,text/plain,*/*")
                .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build();
    }

    /**
     * 始终启用。
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * 搜索图片并下载到本地。
     *
     * @param query 搜索关键词（中文英文均可）
     * @param count 需要的图片数量
     * @return 下载成功后的图片相对路径列表（如 ["/uploads/xxx.jpg", ...]）
     */
    public List<String> searchAndDownload(String query, int count) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 确保上传目录存在
        Path uploadDir = Paths.get("uploads").toAbsolutePath();
        try {
            if (Files.notExists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            log.warn("创建上传目录失败：{}", e.getMessage());
            return List.of();
        }

        // 1. 优先 image-crawler 爬虫（用户要求图片时才调用，由调用方控制）
        List<String> results = searchViaBaiduCrawler(query, count, uploadDir);
        if (!results.isEmpty()) {
            return results;
        }
        log.warn("image-crawler 爬取失败或结果为空 [{}]，降级到内置百度搜索", query);

        // 2. 降级内置百度 acjson 搜索
        results = searchViaBaidu(query, count, uploadDir);
        if (!results.isEmpty()) {
            return results;
        }

        // 3. 降级 Pexels
        if (pexelsEnabled) {
            results = searchViaPexels(query, count, uploadDir);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // 4. 最终兜底 LoremFlickr
        return searchViaLoremFlickr(query, count, uploadDir);
    }

    /**
     * 调用 image-crawler Python HTTP API 服务获取图片 URL，再下载到本地。
     * 通过 GET /api/search?keyword=<关键词>&count=<数量> 获取精准图片 URL，
     * 比 Java 内置实现更适合中文关键词搜索。
     */
    private List<String> searchViaBaiduCrawler(String query, int count, Path uploadDir) {
        if (crawlerClient == null) {
            log.info("未配置 image.crawler-api-url，跳过 image-crawler API");
            return List.of();
        }
        try {
            String response = crawlerClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/search")
                            .queryParam("keyword", query)
                            .queryParam("count", count)
                            .build())
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                log.warn("image-crawler API 返回空响应 [{}]", query);
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode urlsNode = root.path("urls");
            List<String> urls = new ArrayList<>();
            if (urlsNode.isArray()) {
                for (JsonNode n : urlsNode) {
                    String u = n.asText("");
                    if (!u.isBlank()) urls.add(u);
                }
            }
            if (urls.isEmpty()) {
                log.warn("image-crawler API 未返回图片 URL [{}]：{}", query,
                        response.length() > 200 ? response.substring(0, 200) : response);
                return List.of();
            }
            log.info("image-crawler API：关键词 [{}] 爬取到 {} 个图片 URL，开始下载", query, urls.size());

            List<String> results = new ArrayList<>();
            for (String url : urls) {
                if (results.size() >= count) break;
                String localPath = downloadImage(url, uploadDir);
                if (localPath != null) {
                    results.add(localPath);
                    log.info("image-crawler API：已下载 [{}] #{} -> {}", query, results.size(), localPath);
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("image-crawler API 调用异常 [{}]：{}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * 百度图片搜索结果条目（来自 acjson 结构化接口）。
     * objURL 为原图，middleURL/hoverURL/thumbURL 为不同尺寸的代理图。
     */
    private record BaiduImageItem(String objURL, String middleURL, String hoverURL,
                                  String thumbURL, int width, int height, String adType) {}

    /**
     * 百度图片搜索：
     * 使用 acjson 结构化接口（返回带尺寸、广告标记的 JSON），比解析 HTML 页面可靠。
     * 过滤广告图和尺寸过小的图（logo/头像/商品缩略图），避免搜到与关键词无关的图片。
     */
    private List<String> searchViaBaidu(String query, int count, Path uploadDir) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(
                    "/search/acjson?tn=resultjson_com&ipn=rj&ct=201326592&is=&fp=result&queryWord=%s&cl=2&lm=-1&ie=utf-8&oe=utf-8&word=%s&pn=0&rn=30&face=0&istype=2",
                    encodedQuery, encodedQuery);

            String response = baiduClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("百度搜索返回空响应 [{}]", query);
                return List.of();
            }

            List<BaiduImageItem> items = extractBaiduItems(response);
            if (items.isEmpty()) {
                log.warn("百度搜索未提取到图片数据 [{}]", query);
                return List.of();
            }

            // 过滤：剔除广告；剔除尺寸过小的图（logo/头像/商品缩略图常小于此尺寸）
            List<BaiduImageItem> filtered = items.stream()
                    .filter(it -> it.adType() == null || "0".equals(it.adType()))
                    .filter(it -> it.width() == 0 || it.height() == 0
                            || (it.width() >= 500 && it.height() >= 300))
                    .toList();
            log.info("百度搜索 [{}]：共 {} 条，过滤广告/小图后 {} 条可下载", query, items.size(), filtered.size());

            List<String> results = new ArrayList<>();
            for (BaiduImageItem item : filtered) {
                if (results.size() >= count) break;
                // 依次尝试 objURL（原图）→ middleURL → hoverURL → thumbURL
                // 单条 URL 非法（相对路径/非法字符）只跳过该条，不让整个搜索失败
                String localPath = null;
                String usedHost = "";
                for (String candidate : new String[]{item.objURL(), item.middleURL(), item.hoverURL(), item.thumbURL()}) {
                    if (candidate == null || candidate.isBlank()) continue;
                    if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) continue;
                    localPath = downloadImage(candidate, uploadDir);
                    if (localPath != null) {
                        usedHost = safeHost(candidate);
                        break;
                    }
                }
                if (localPath != null) {
                    results.add(localPath);
                    log.info("百度搜索：已下载 [{}] #{} <- {} -> {}", query, results.size(), usedHost, localPath);
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("百度搜索失败 [{}]：{}，降级到下一个来源", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * 从百度 acjson 响应中解析图片条目列表。
     * 响应偶尔非严格 JSON（前后有杂质或 JSONP 包裹），先截取最外层 {} 再解析。
     */
    private List<BaiduImageItem> extractBaiduItems(String response) {
        List<BaiduImageItem> items = new ArrayList<>();
        try {
            String json = response.trim();
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                data = root.path("list");
            }

            for (JsonNode n : data) {
                String thumbURL = n.path("thumbURL").asText("");
                String objURL = decodeBaiduUrl(n.path("objURL").asText(""));
                String middleURL = decodeBaiduUrl(n.path("middleURL").asText(""));
                String hoverURL = decodeBaiduUrl(n.path("hoverURL").asText(""));
                if (thumbURL.isBlank() && objURL.isBlank() && middleURL.isBlank()) continue;
                items.add(new BaiduImageItem(
                        objURL, middleURL, hoverURL, thumbURL,
                        n.path("width").asInt(0), n.path("height").asInt(0),
                        n.path("adType").asText("0")));
            }
        } catch (Exception e) {
            log.warn("解析百度 acjson 响应失败：{}", e.getMessage());
        }
        return items;
    }

    /**
     * 百度 acjson 中的 objURL 有时是 URL 编码形式（http%3A%2F%2F...），需要解码。
     */
    private String decodeBaiduUrl(String url) {
        if (url == null || url.isBlank()) return "";
        if (url.contains("%")) {
            try {
                return URLDecoder.decode(url, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return url;
    }

    /**
     * Pexels API 搜索：高质量，按关键词匹配。
     */
    private List<String> searchViaPexels(String query, int count, Path uploadDir) {
        try {
            String response = pexelsClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("query", query)
                            .queryParam("per_page", count)
                            .queryParam("orientation", "landscape")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode photos = root.path("photos");

            List<String> results = new ArrayList<>();
            for (JsonNode photo : photos) {
                if (results.size() >= count) break;
                String imageUrl = photo.path("src").path("large2x").asText(
                        photo.path("src").path("large").asText(""));
                if (imageUrl.isBlank()) continue;

                String localPath = downloadImage(imageUrl, uploadDir);
                if (localPath != null) {
                    results.add(localPath);
                    log.info("Pexels 搜索：已下载 [{}] -> {}", query, localPath);
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Pexels 搜索失败 [{}]：{}，降级为 LoremFlickr", query, e.getMessage());
            return searchViaLoremFlickr(query, count, uploadDir);
        }
    }

    /**
     * LoremFlickr 搜索：免认证，基于 Flickr 关键词。
     * 每张图片用不同 seed 获取不同结果。
     */
    private List<String> searchViaLoremFlickr(String query, int count, Path uploadDir) {
        List<String> results = new ArrayList<>();
        String keywords = query.replace(" ", ",");

        for (int i = 0; i < count; i++) {
            int width = 1200;
            int height = i == 0 ? 600 : 800;
            int lock = Math.abs((query + i).hashCode());
            String url = String.format("https://loremflickr.com/%d/%d/%s?lock=%d",
                    width, height, keywords, lock);

            String localPath = downloadImage(url, uploadDir);
            if (localPath != null) {
                results.add(localPath);
                log.info("LoremFlickr 搜索：已下载 [{}] #{} -> {}", query, i + 1, localPath);
            }
        }
        return results;
    }

    /**
     * 下载图片到本地 uploads/ 目录，返回相对路径。
     * 设置浏览器 User-Agent 和 Referer 避免被某些 CDN 拒绝。
     * 下载后校验图片魔数与最小体积，过滤无效内容、跟踪像素和超小图标。
     */
    private String downloadImage(String imageUrl, Path uploadDir) {
        try {
            URL url = URI.create(imageUrl).toURL();
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://image.baidu.com/");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            // 先读到内存校验，通过后再落盘，避免存入无效内容
            byte[] bytes;
            try (InputStream in = conn.getInputStream()) {
                bytes = in.readAllBytes();
            }
            if (!isValidImage(bytes)) {
                log.warn("下载内容不是有效图片或体积过小（{} 字节）：{}", bytes.length, imageUrl);
                return null;
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            Path dest = uploadDir.resolve(filename);
            Files.write(dest, bytes);
            return "/uploads/" + filename;
        } catch (Exception e) {
            // URL 非法（相对路径/非法字符）或网络失败，仅跳过该条，不影响其他候选
            log.warn("下载图片失败：{} - {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 安全提取 URL 的域名，非法 URL 返回空串。
     */
    private String safeHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null ? host : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 校验字节数据是否为有效图片（JPEG/PNG/WebP/GIF）且体积不小于 20KB。
     */
    private boolean isValidImage(byte[] bytes) {
        if (bytes == null || bytes.length < 20_000) return false;
        int b0 = bytes[0] & 0xFF, b1 = bytes[1] & 0xFF;
        // JPEG: FF D8
        if (b0 == 0xFF && b1 == 0xD8) return true;
        // PNG: 89 50
        if (b0 == 0x89 && b1 == 0x50) return true;
        // GIF: "GI"
        if (b0 == 'G' && b1 == 'I') return true;
        // WebP: RIFF....WEBP
        if (bytes.length > 12 && b0 == 0x52 && b1 == 0x49
                && (bytes[8] & 0xFF) == 0x57 && (bytes[11] & 0xFF) == 0x50) return true;
        return false;
    }
}
