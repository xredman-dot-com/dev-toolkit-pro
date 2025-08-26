package com.devtoolkit.pro.actions;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
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
            // 打开Search Everywhere对话框并直接定位到Restful Endpoints标签页
            SearchEverywhereManager manager = SearchEverywhereManager.getInstance(project);
            if (manager != null) {
                // 使用正确的贡献者ID来直接打开Restful Endpoints标签页
                manager.show("DevToolkitPro.RestfulEndpoints", "", e);
                LOG.info("Successfully opened Search Everywhere dialog with Restful Endpoints tab");
                return;
            } else {
                LOG.warn("SearchEverywhereManager is null");
            }
        } catch (Exception ex) {
            LOG.error("Error showing Search Everywhere with Restful Endpoints tab", ex);
            // 如果出错，尝试使用默认的All标签页作为回退
            try {
                SearchEverywhereManager manager = SearchEverywhereManager.getInstance(project);
                if (manager != null) {
                    manager.show("All", "", e);
                    LOG.info("Fallback: opened Search Everywhere with All tab");
                }
            } catch (Exception fallbackEx) {
                LOG.error("Fallback also failed", fallbackEx);
            }
        }
    }



    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}