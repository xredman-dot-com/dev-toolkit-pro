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
import com.intellij.util.Processor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RESTful端点搜索贡献者
 * 为Search Everywhere对话框提供"Restful Endpoints"标签页
 */
public class RestfulEndpointSearchEverywhereContributor implements SearchEverywhereContributor<RestfulEndpointNavigationItem> {
    
    private static final Logger LOG = Logger.getInstance(RestfulEndpointSearchEverywhereContributor.class);
    
    private final Project project;
    
    public RestfulEndpointSearchEverywhereContributor(@NotNull AnActionEvent initEvent) {
        this.project = initEvent.getProject();
    }

    @NotNull
    @Override
    public String getSearchProviderId() {
        return "RestfulEndpoints";
    }

    @NotNull
    @Override
    public String getGroupName() {
        return "Restful Endpoints";
    }

    @Override
    public int getSortWeight() {
        return 700; // 设置更高的排序权重，确保显示在标签栏中
    }

    @Override
    public boolean showInFindResults() {
        return true;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true; // 确保显示为单独的标签页
    }

    @Override
    public void fetchElements(@NotNull String pattern,
                            @NotNull ProgressIndicator progressIndicator,
                            @NotNull Processor<? super RestfulEndpointNavigationItem> consumer) {
        if (project == null) {
            return;
        }

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                LOG.info("Fetching RESTful endpoints for pattern: " + pattern);
                
                RestfulUrlService urlService = new RestfulUrlService(project);
                List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
                
                // 过滤匹配的端点
                for (RestfulEndpointNavigationItem endpoint : endpoints) {
                    if (progressIndicator.isCanceled()) {
                        break;
                    }
                    
                    // 简单的模糊匹配
                    if (pattern.isEmpty() || matchesPattern(endpoint.getName(), pattern)) {
                        consumer.process(endpoint);
                    }
                }
                
                LOG.info("Processed " + endpoints.size() + " endpoints for pattern: " + pattern);
            } catch (Exception e) {
                LOG.error("Error fetching RESTful endpoints", e);
            }
        });
    }

    @Override
    public boolean processSelectedItem(@NotNull RestfulEndpointNavigationItem selected, int modifiers, @NotNull String searchText) {
        // 导航到选中的端点
        selected.navigate(true);
        return true;
    }

    @Override
    public @Nullable Object getDataForItem(@NotNull RestfulEndpointNavigationItem element, @NotNull String dataId) {
        return null;
    }

    @Override
    public boolean isEmptyPatternSupported() {
        return true; // 支持空模式搜索，显示所有端点
    }

    @Override
    public boolean isDumbModeSupported() {
        return true; // 在dumb模式下也支持
    }

    @NotNull
    @Override
    public ListCellRenderer<? super RestfulEndpointNavigationItem> getElementsRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (value instanceof RestfulEndpointNavigationItem) {
                    RestfulEndpointNavigationItem item = (RestfulEndpointNavigationItem) value;
                    setText(item.getName());
                    // 使用getPresentation()获取图标
                    if (item.getPresentation() != null) {
                        setIcon(item.getPresentation().getIcon(false));
                    }
                }
                
                return this;
            }
        };
    }

    /**
     * 简单的模糊匹配算法
     */
    private boolean matchesPattern(String text, String pattern) {
        if (pattern.isEmpty()) {
            return true;
        }
        
        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase();
        
        // 包含匹配
        if (lowerText.contains(lowerPattern)) {
            return true;
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
     * 工厂类用于创建贡献者实例
     */
    public static class Factory implements SearchEverywhereContributorFactory<RestfulEndpointNavigationItem> {
        @NotNull
        @Override
        public SearchEverywhereContributor<RestfulEndpointNavigationItem> createContributor(@NotNull AnActionEvent initEvent) {
            return new RestfulEndpointSearchEverywhereContributor(initEvent);
        }
    }
}