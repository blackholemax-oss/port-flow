package dev.blackholemax.backend.web;

/**
 * 表单 XSS 过滤工具：
 * 对用户输入的文本字段做 HTML 实体编码，防止 <script> 注入、on* 事件处理器等攻击。
 * 注意：generatedHtml 字段不过滤（由 AI 生成，且在 iframe sandbox 中渲染）。
 */
public final class XssFilter {

    private XssFilter() {}

    /**
     * 对用户输入的纯文本字段做 HTML 实体编码。
     * null 原样返回；空字符串原样返回。
     */
    public static String clean(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                case '&' -> sb.append("&amp;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 过滤列表中的每个字符串（用于项目标题/描述列表）。
     */
    public static java.util.List<String> cleanList(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return list;
        java.util.List<String> result = new java.util.ArrayList<>(list.size());
        for (String s : list) {
            result.add(clean(s));
        }
        return result;
    }
}
