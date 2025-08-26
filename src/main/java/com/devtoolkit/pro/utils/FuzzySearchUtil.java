package com.devtoolkit.pro.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模糊搜索工具类
 * 实现高效的模糊匹配算法
 */
public class FuzzySearchUtil {

    /**
     * 执行模糊搜索
     * @param items 待搜索的项目列表
     * @param query 搜索查询字符串
     * @return 按相关性排序的搜索结果
     */
    public static List<String> fuzzySearch(List<String> items, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(items);
        }

        String normalizedQuery = query.toLowerCase().trim();
        
        return items.stream()
                .map(item -> new SearchResult(item, calculateScore(item, normalizedQuery)))
                .filter(result -> result.score > 0)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .map(result -> result.item)
                .collect(Collectors.toList());
    }

    /**
     * 计算搜索项与查询字符串的匹配分数
     */
    private static double calculateScore(String item, String query) {
        if (item == null || item.isEmpty()) {
            return 0.0;
        }

        String normalizedItem = item.toLowerCase();
        double score = 0.0;

        // 1. 完全匹配得分最高
        if (normalizedItem.equals(query)) {
            return 1000.0;
        }

        // 2. 开头匹配得分较高
        if (normalizedItem.startsWith(query)) {
            score += 500.0;
        }

        // 3. 包含完整查询字符串
        if (normalizedItem.contains(query)) {
            score += 300.0;
        }

        // 4. 连续字符匹配
        score += calculateConsecutiveMatches(normalizedItem, query) * 50.0;

        // 5. 字符顺序匹配（不一定连续）
        score += calculateSequentialMatches(normalizedItem, query) * 20.0;

        // 6. 单个字符匹配
        score += calculateCharacterMatches(normalizedItem, query) * 5.0;

        // 7. 路径分段匹配（针对URL路径）
        score += calculatePathSegmentMatches(normalizedItem, query) * 30.0;

        // 8. HTTP方法匹配
        score += calculateHttpMethodMatches(normalizedItem, query) * 100.0;

        // 9. 长度惩罚（较短的匹配项得分更高）
        if (score > 0) {
            double lengthPenalty = Math.max(0, (normalizedItem.length() - query.length()) * 0.1);
            score = Math.max(1.0, score - lengthPenalty);
        }

        return score;
    }

    /**
     * 计算连续字符匹配得分
     */
    private static double calculateConsecutiveMatches(String item, String query) {
        double score = 0.0;
        int queryIndex = 0;
        int consecutiveCount = 0;
        int maxConsecutive = 0;

        for (int i = 0; i < item.length() && queryIndex < query.length(); i++) {
            if (item.charAt(i) == query.charAt(queryIndex)) {
                consecutiveCount++;
                queryIndex++;
            } else {
                maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
                consecutiveCount = 0;
            }
        }
        maxConsecutive = Math.max(maxConsecutive, consecutiveCount);

        return (double) maxConsecutive / query.length();
    }

    /**
     * 计算字符顺序匹配得分
     */
    private static double calculateSequentialMatches(String item, String query) {
        int queryIndex = 0;
        int matchCount = 0;

        for (int i = 0; i < item.length() && queryIndex < query.length(); i++) {
            if (item.charAt(i) == query.charAt(queryIndex)) {
                matchCount++;
                queryIndex++;
            }
        }

        return (double) matchCount / query.length();
    }

    /**
     * 计算单个字符匹配得分
     */
    private static double calculateCharacterMatches(String item, String query) {
        Set<Character> itemChars = new HashSet<>();
        Set<Character> queryChars = new HashSet<>();

        for (char c : item.toCharArray()) {
            itemChars.add(c);
        }
        for (char c : query.toCharArray()) {
            queryChars.add(c);
        }

        int matchCount = 0;
        for (char c : queryChars) {
            if (itemChars.contains(c)) {
                matchCount++;
            }
        }

        return (double) matchCount / queryChars.size();
    }

    /**
     * 计算路径分段匹配得分（针对URL路径）
     */
    private static double calculatePathSegmentMatches(String item, String query) {
        // 提取URL路径部分
        String path = extractUrlPath(item);
        if (path.isEmpty()) {
            return 0.0;
        }

        String[] pathSegments = path.split("/");
        double maxScore = 0.0;

        for (String segment : pathSegments) {
            if (segment.toLowerCase().contains(query)) {
                double segmentScore = (double) query.length() / segment.length();
                maxScore = Math.max(maxScore, segmentScore);
            }
        }

        return maxScore;
    }

    /**
     * 计算HTTP方法匹配得分
     */
    private static double calculateHttpMethodMatches(String item, String query) {
        String[] httpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};
        
        for (String method : httpMethods) {
            if (method.toLowerCase().startsWith(query) && item.toLowerCase().contains(method.toLowerCase())) {
                return 1.0;
            }
        }
        
        return 0.0;
    }

    /**
     * 从URL信息中提取路径部分
     */
    private static String extractUrlPath(String urlInfo) {
        // URL信息格式: "GET /api/users (UserController.getUsers)"
        int spaceIndex = urlInfo.indexOf(' ');
        int parenIndex = urlInfo.indexOf('(');
        
        if (spaceIndex > 0 && parenIndex > spaceIndex) {
            return urlInfo.substring(spaceIndex + 1, parenIndex).trim();
        }
        
        return "";
    }

    /**
     * 搜索结果内部类
     */
    private static class SearchResult {
        final String item;
        final double score;

        SearchResult(String item, double score) {
            this.item = item;
            this.score = score;
        }
    }

    /**
     * 高亮匹配的字符
     * @param text 原始文本
     * @param query 查询字符串
     * @return 带有高亮标记的文本
     */
    public static String highlightMatches(String text, String query) {
        if (query == null || query.trim().isEmpty()) {
            return text;
        }

        String normalizedText = text.toLowerCase();
        String normalizedQuery = query.toLowerCase();
        StringBuilder result = new StringBuilder();
        
        int queryIndex = 0;
        for (int i = 0; i < text.length() && queryIndex < normalizedQuery.length(); i++) {
            char currentChar = text.charAt(i);
            char normalizedChar = normalizedText.charAt(i);
            
            if (normalizedChar == normalizedQuery.charAt(queryIndex)) {
                result.append("<b>").append(currentChar).append("</b>");
                queryIndex++;
            } else {
                result.append(currentChar);
            }
        }
        
        // 添加剩余字符
        if (result.length() < text.length()) {
            result.append(text.substring(result.toString().replaceAll("<[^>]*>", "").length()));
        }
        
        return result.toString();
    }
}