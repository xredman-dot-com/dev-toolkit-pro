package com.devtoolkit.pro.contributors;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.services.RestfulUrlService;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.util.List;

/**
 * RESTful端点搜索贡献者
 * 为Search Everywhere对话框提供"Restful Endpoints"标签页
 */
public class RestfulEndpointSearchEverywhereContributor implements SearchEverywhereContributor<RestfulEndpointNavigationItem> {
    
    private static final Logger LOG = Logger.getInstance(RestfulEndpointSearchEverywhereContributor.class);
    
    private final Project project;
    private String currentSearchPattern = ""; // 存储当前搜索模式
    private RestfulEndpointRenderer renderer; // 渲染器实例
    
    public RestfulEndpointSearchEverywhereContributor(@NotNull AnActionEvent initEvent) {
        this.project = initEvent.getProject();
        LOG.info("RestfulEndpointSearchEverywhereContributor created for project: " + 
                (project != null ? project.getName() : "null"));
    }

    @NotNull
    @Override
    public String getSearchProviderId() {
        return "DevToolkitPro.RestfulEndpoints"; // 使用插件前缀避免冲突
    }

    @NotNull
    @Override
    public String getGroupName() {
        return "Restful Endpoints";
    }

    @Override
    public int getSortWeight() {
        return 700;
    }

    @Override
    public boolean showInFindResults() {
        return true; // 改为true，确保能被搜索到
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true; // 确保作为独立标签页显示
    }

    @Override
    public void fetchElements(@NotNull String pattern,
                            @NotNull ProgressIndicator progressIndicator,
                            @NotNull Processor<? super RestfulEndpointNavigationItem> consumer) {
        LOG.info("fetchElements called with pattern: '" + pattern + "'");
        
        // 保存当前搜索模式用于高亮显示
        this.currentSearchPattern = pattern;
        if (renderer != null) {
            renderer.setCurrentSearchPattern(pattern);
        }
        
        if (project == null) {
            LOG.warn("Project is null, cannot fetch endpoints");
            return;
        }

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                RestfulUrlService urlService = new RestfulUrlService(project);
                List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
                
                LOG.info("Found " + endpoints.size() + " total endpoints");
                
                int processedCount = 0;
                for (RestfulEndpointNavigationItem endpoint : endpoints) {
                    if (progressIndicator.isCanceled()) {
                        LOG.info("Search canceled by user");
                        break;
                    }
                    
                    if (pattern.isEmpty() || matchesPattern(endpoint.getName(), pattern)) {
                        consumer.process(endpoint);
                        processedCount++;
                    }
                }
                
                LOG.info("Processed " + processedCount + " matching endpoints for pattern: '" + pattern + "'");
            } catch (Exception e) {
                LOG.error("Error fetching RESTful endpoints", e);
            }
        });
    }

    @Override
    public boolean processSelectedItem(@NotNull RestfulEndpointNavigationItem selected, int modifiers, @NotNull String searchText) {
        LOG.info("Processing selected item: " + selected.getName() + ", modifiers: " + modifiers);
        
        // 检查是否按下了Shift键 (Shift+Enter)
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            // 复制完整的URL和方法到剪切板
            String fullUrl = selected.getName(); // 格式如: "GET /api/users"
            try {
                CopyPasteManager.getInstance().setContents(new StringSelection(fullUrl));
                LOG.info("Copied to clipboard: " + fullUrl);
                return true; // 返回true表示已处理，不进行导航
            } catch (Exception e) {
                LOG.error("Error copying to clipboard", e);
                return false;
            }
        } else {
            // 正常的Enter键，进行导航
            try {
                selected.navigate(true);
                return true;
            } catch (Exception e) {
                LOG.error("Error navigating to selected item", e);
                return false;
            }
        }
    }

    @Override
    public @Nullable Object getDataForItem(@NotNull RestfulEndpointNavigationItem element, @NotNull String dataId) {
        return null;
    }

    @NotNull
    @Override
    public ListCellRenderer<? super RestfulEndpointNavigationItem> getElementsRenderer() {
        if (renderer == null) {
            renderer = new RestfulEndpointRenderer();
            renderer.setCurrentSearchPattern(currentSearchPattern);
        }
        return renderer;
    }
    
    /**
     * 自定义渲染器，支持高亮匹配的文本部分
     */
    private class RestfulEndpointRenderer extends DefaultListCellRenderer {
        private String currentSearchPattern = "";
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof RestfulEndpointNavigationItem) {
                RestfulEndpointNavigationItem item = (RestfulEndpointNavigationItem) value;
                String itemName = item.getName();
                
                // 设置图标
                if (item.getPresentation() != null) {
                    setIcon(item.getPresentation().getIcon(false));
                }
                
                // 获取当前搜索模式（从fetchElements传递过来的pattern）
                String highlightedText = highlightMatchingText(itemName, getCurrentSearchPattern());
                setText(highlightedText);
            }
            
            return this;
        }
        
        /**
         * 高亮匹配的文本部分
         */
        private String highlightMatchingText(String text, String pattern) {
            if (pattern == null || pattern.isEmpty()) {
                return text;
            }
            
            String lowerText = text.toLowerCase();
            String lowerPattern = pattern.toLowerCase();
            
            // 处理查询参数的情况
            String searchPattern = lowerPattern;
            if (lowerPattern.contains("?")) {
                searchPattern = lowerPattern.split("\\?")[0]; // 只高亮基础路径部分
            }
            
            // 查找匹配的位置并高亮
            int startIndex = lowerText.indexOf(searchPattern);
            if (startIndex != -1) {
                StringBuilder highlighted = new StringBuilder("<html>");
                highlighted.append(text, 0, startIndex);
                highlighted.append("<b><font color='#4A90E2'>"); // 蓝色高亮
                highlighted.append(text, startIndex, startIndex + searchPattern.length());
                highlighted.append("</font></b>");
                highlighted.append(text.substring(startIndex + searchPattern.length()));
                highlighted.append("</html>");
                return highlighted.toString();
            }
            
            return text;
        }
        
        /**
         * 获取当前搜索模式
         */
        private String getCurrentSearchPattern() {
            return currentSearchPattern;
        }
        
        /**
         * 设置当前搜索模式
         */
        public void setCurrentSearchPattern(String pattern) {
            this.currentSearchPattern = pattern;
        }
    }

    /**
     * 增强的模糊匹配算法，支持查询参数匹配
     */
    private boolean matchesPattern(String text, String pattern) {
        if (pattern.isEmpty()) {
            return true;
        }
        
        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase();
        
        // 直接包含匹配
        if (lowerText.contains(lowerPattern)) {
            return true;
        }
        
        // 处理查询参数匹配：如果pattern包含查询参数，尝试匹配基础路径
        if (lowerPattern.contains("?")) {
            // 提取pattern中的基础路径部分（去掉查询参数）
            String patternBasePath = lowerPattern.split("\\?")[0];
            
            // 从text中提取路径部分（去掉HTTP方法）
            String textPath = extractPathFromEndpoint(lowerText);
            
            // 检查基础路径是否匹配
            if (textPath.equals(patternBasePath) || textPath.contains(patternBasePath)) {
                return true;
            }
        }
        
        // 反向匹配：如果text是基础路径，pattern包含查询参数，也应该匹配
        String textPath = extractPathFromEndpoint(lowerText);
        if (lowerPattern.contains("?")) {
            String patternBasePath = lowerPattern.split("\\?")[0];
            if (textPath.equals(patternBasePath)) {
                return true;
            }
        }
        
        // 模糊匹配：检查模式中的字符是否按顺序出现在文本中
        int patternIndex = 0;
        for (int i = 0; i < lowerText.length() && patternIndex < lowerPattern.length(); i++) {
            if (lowerText.charAt(i) == lowerPattern.charAt(patternIndex)) {
                patternIndex++;
            }
        }
        
        return patternIndex == lowerPattern.length();
    }
    
    /**
     * 从端点文本中提取路径部分
     * 例如："GET /api/users" -> "/api/users"
     */
    private String extractPathFromEndpoint(String endpointText) {
        if (endpointText == null || endpointText.trim().isEmpty()) {
            return "";
        }
        
        // 查找第一个空格，HTTP方法后面就是路径
        int spaceIndex = endpointText.indexOf(' ');
        if (spaceIndex != -1 && spaceIndex < endpointText.length() - 1) {
            return endpointText.substring(spaceIndex + 1).trim();
        }
        
        // 如果没有空格，可能整个字符串就是路径
        return endpointText.trim();
    }

    /**
     * 工厂类用于创建贡献者实例
     */
    public static class Factory implements SearchEverywhereContributorFactory<RestfulEndpointNavigationItem> {
        private static final Logger FACTORY_LOG = Logger.getInstance(Factory.class);
        
        @NotNull
        @Override
        public SearchEverywhereContributor<RestfulEndpointNavigationItem> createContributor(@NotNull AnActionEvent initEvent) {
            FACTORY_LOG.info("Creating RestfulEndpointSearchEverywhereContributor via Factory");
            return new RestfulEndpointSearchEverywhereContributor(initEvent);
        }
    }
}