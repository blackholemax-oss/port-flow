package dev.blackholemax.backend.service;

import dev.blackholemax.backend.entity.StylePrompt;
import dev.blackholemax.backend.repository.StylePromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 轻量 RAG 检索：基于 TF-IDF 余弦相似度匹配用户输入与风格提示词库。
 * 不依赖外部向量数据库或 embedding API，纯内存计算，零外部依赖。
 *
 * 检索流程：
 * 1. 用户输入（灵感便签/自定义提示词）分词
 * 2. 与每条风格的关键词 + 描述做 TF-IDF 相似度计算
 * 3. 返回 top-K 最相关的风格提示词
 */
@Service
public class StyleRagService {

    private static final Logger log = LoggerFactory.getLogger(StyleRagService.class);
    private static final int DEFAULT_TOP_K = 3;

    private final StylePromptRepository stylePromptRepository;
    // 缓存 IDF 权重，避免每次检索重算
    private volatile boolean indexed = false;
    private final Map<String, Double> idfCache = new HashMap<>();
    private final Map<Long, Map<String, Double>> docVectors = new HashMap<>();

    public StyleRagService(StylePromptRepository stylePromptRepository) {
        this.stylePromptRepository = stylePromptRepository;
    }

    /**
     * 检索与输入最相关的前 K 个风格提示词。
     */
    public List<StylePrompt> retrieve(String input, int topK) {
        ensureIndexed();
        if (input == null || input.isBlank()) {
            return stylePromptRepository.findAll().stream().limit(topK).collect(Collectors.toList());
        }

        List<String> queryTerms = tokenize(input);
        if (queryTerms.isEmpty()) {
            return stylePromptRepository.findAll().stream().limit(topK).collect(Collectors.toList());
        }

        Map<String, Double> queryVector = computeTfIdf(queryTerms, queryTerms.size());

        List<StylePrompt> all = stylePromptRepository.findAll();
        List<ScoredStyle> scored = new ArrayList<>();
        for (StylePrompt style : all) {
            Map<String, Double> docVec = docVectors.get(style.getId());
            if (docVec == null) continue;
            double similarity = cosineSimilarity(queryVector, docVec);
            scored.add(new ScoredStyle(style, similarity));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.stream().limit(topK).map(s -> s.style).collect(Collectors.toList());
    }

    /** 默认检索 top-3 */
    public List<StylePrompt> retrieve(String input) {
        return retrieve(input, DEFAULT_TOP_K);
    }

    /** 获取所有可用风格（供前端展示风格列表） */
    public List<StylePrompt> allStyles() {
        return stylePromptRepository.findAll();
    }

    private synchronized void ensureIndexed() {
        if (indexed) return;
        List<StylePrompt> all = stylePromptRepository.findAll();
        if (all.isEmpty()) {
            indexed = true;
            return;
        }

        // 统计文档频率（每个词出现在多少篇文档中）
        Map<String, Integer> docFreq = new HashMap<>();
        int totalDocs = all.size();

        Map<Long, List<String>> docTerms = new HashMap<>();
        for (StylePrompt style : all) {
            // 文档内容 = 关键词 + 描述 + 名称
            String docText = (style.getKeywords() != null ? style.getKeywords() : "") + " "
                    + (style.getDescription() != null ? style.getDescription() : "") + " "
                    + (style.getName() != null ? style.getName() : "");
            List<String> terms = tokenize(docText);
            docTerms.put(style.getId(), terms);
            Set<String> uniqueTerms = new HashSet<>(terms);
            for (String term : uniqueTerms) {
                docFreq.merge(term, 1, Integer::sum);
            }
        }

        // 计算 IDF: log(N / df)
        idfCache.clear();
        for (Map.Entry<String, Integer> e : docFreq.entrySet()) {
            idfCache.put(e.getKey(), Math.log((double) totalDocs / e.getValue()));
        }

        // 计算每篇文档的 TF-IDF 向量
        docVectors.clear();
        for (StylePrompt style : all) {
            List<String> terms = docTerms.get(style.getId());
            docVectors.put(style.getId(), computeTfIdf(terms, terms.size()));
        }

        indexed = true;
        log.info("RAG 索引完成：{} 条风格提示词，{} 个唯一词", totalDocs, idfCache.size());
    }

    /** 重建索引（新增风格后调用） */
    public void rebuildIndex() {
        indexed = false;
        ensureIndexed();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        // 中英文混合分词：英文按空格/标点，中文按字符
        List<String> tokens = new ArrayList<>();
        // 先提取英文单词
        String[] englishWords = text.toLowerCase().split("[^a-z0-9]+");
        for (String w : englishWords) {
            if (w.length() > 1) tokens.add(w);
        }
        // 再提取中文字符（2-gram）
        String chineseOnly = text.replaceAll("[^\\u4e00-\\u9fa5]", "");
        for (int i = 0; i < chineseOnly.length() - 1; i++) {
            tokens.add(chineseOnly.substring(i, i + 2));
        }
        // 单字也加入
        for (int i = 0; i < chineseOnly.length(); i++) {
            tokens.add(String.valueOf(chineseOnly.charAt(i)));
        }
        return tokens;
    }

    private Map<String, Double> computeTfIdf(List<String> terms, int totalTerms) {
        Map<String, Double> tf = new HashMap<>();
        for (String term : terms) {
            tf.merge(term, 1.0, Double::sum);
        }
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Double> e : tf.entrySet()) {
            double tfVal = e.getValue() / totalTerms;
            double idfVal = idfCache.getOrDefault(e.getKey(), 0.0);
            vector.put(e.getKey(), tfVal * idfVal);
        }
        return vector;
    }

    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Map.Entry<String, Double> e : v1.entrySet()) {
            norm1 += e.getValue() * e.getValue();
            Double v2Val = v2.get(e.getKey());
            if (v2Val != null) {
                dotProduct += e.getValue() * v2Val;
            }
        }
        for (double val : v2.values()) {
            norm2 += val * val;
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private record ScoredStyle(StylePrompt style, double score) {
    }
}
