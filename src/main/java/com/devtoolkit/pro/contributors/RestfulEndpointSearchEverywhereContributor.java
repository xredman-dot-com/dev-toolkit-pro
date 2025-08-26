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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.util.List;

/**
 * RESTful端点搜索贡献者
 * 为Search Everywhere对话框提供"Restful Endpoints"标签页
 */
public class RestfulEndpointSearchEverywhereContributor implements SearchEverywhereContributor<RestfulEndpointNavigationItem> {
    
    private static final Logger LOG = Logger.getInstance(RestfulEndpointSearchEverywhereContributor.class);
    
    private final Project project;
    
    public RestfulEndpointSearchEverywhereContributor(@NotNull AnActionEvent initEvent) {
        this.project = initEvent.getProject();
        LOG.info("RestfulEndpointSearchEverywhereContributor created for project: " + 
                (project != null ? project.getName() : "null"));
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
        return 700;
    }

    @Override
    public boolean showInFindResults() {
        return false; // 不在Find Results中显示，只在独立标签页中显示
    }

    @Override
    public void fetchElements(@NotNull String pattern,
                            @NotNull ProgressIndicator progressIndicator,
                            @NotNull Processor<? super RestfulEndpointNavigationItem> consumer) {
        LOG.info("fetchElements called with pattern: '" + pattern + "'");
        
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
        LOG.info("Processing selected item: " + selected.getName());
        try {
            selected.navigate(true);
            return true;
        } catch (Exception e) {
            LOG.error("Error navigating to selected item", e);
            return false;
        }
    }

    @Override
    public @Nullable Object getDataForItem(@NotNull RestfulEndpointNavigationItem element, @NotNull String dataId) {
        return null;
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
        private static final Logger FACTORY_LOG = Logger.getInstance(Factory.class);
        
        @NotNull
        @Override
        public SearchEverywhereContributor<RestfulEndpointNavigationItem> createContributor(@NotNull AnActionEvent initEvent) {
            FACTORY_LOG.info("Creating RestfulEndpointSearchEverywhereContributor via Factory");
            return new RestfulEndpointSearchEverywhereContributor(initEvent);
        }
    }
}