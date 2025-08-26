package com.devtoolkit.pro.actions;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 专门的RESTful端点Search Everywhere动作
 * 直接打开Search Everywhere并切换到Restful Endpoints标签页
 */
public class SearchRestfulEndpointsEverywhereAction extends AnAction {
    
    private static final Logger LOG = Logger.getInstance(SearchRestfulEndpointsEverywhereAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            LOG.warn("No project available for SearchRestfulEndpointsEverywhereAction");
            return;
        }

        LOG.info("Triggering Search Everywhere for Restful Endpoints");

        try {
            // 首先尝试直接使用特定ID打开
            SearchEverywhereManager manager = SearchEverywhereManager.getInstance(project);
            if (manager != null) {
                try {
                    // 尝试使用类名ID
                    manager.show("RestfulEndpointSearchEverywhereContributor", "", e);
                    LOG.info("Successfully triggered Search Everywhere with class name ID");
                    return;
                } catch (IllegalArgumentException ex1) {
                    LOG.warn("Class name ID failed, trying short ID: " + ex1.getMessage());
                    try {
                        // 尝试使用短ID
                        manager.show("RestfulEndpoints", "", e);
                        LOG.info("Successfully triggered Search Everywhere with short RestfulEndpoints ID");
                        return;
                    } catch (IllegalArgumentException ex2) {
                        LOG.warn("Short ID also failed: " + ex2.getMessage());
                        // 回退到All标签页
                        manager.show("", "", e);
                        LOG.info("Fallback: Opened Search Everywhere with All tab");
                        return;
                    }
                }
            } else {
                LOG.warn("SearchEverywhereManager is null, falling back to default search");
            }
        } catch (Exception ex) {
            LOG.error("Error showing Search Everywhere", ex);
        }
        
        // 最终回退方案
        fallbackToDefaultSearch(e);
    }

    /**
     * 回退到默认的Search Everywhere
     */
    private void fallbackToDefaultSearch(@NotNull AnActionEvent e) {
        try {
            SearchEverywhereManager manager = SearchEverywhereManager.getInstance(e.getProject());
            if (manager != null) {
                manager.show("", "", e);
                LOG.info("Fallback: Opened default Search Everywhere");
            }
        } catch (Exception ex) {
            LOG.error("Fallback also failed", ex);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}