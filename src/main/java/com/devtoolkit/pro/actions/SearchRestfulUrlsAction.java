package com.devtoolkit.pro.actions;

import com.intellij.ide.actions.SearchEverywhereAction;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 全局RESTful URL搜索动作
 * 通过快捷键触发系统搜索对话框显示RESTful端点
 */
public class SearchRestfulUrlsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        try {
            // 尝试直接使用SearchEverywhereManager切换到我们的标签页
            SearchEverywhereManager manager = SearchEverywhereManager.getInstance(project);
            if (manager != null) {
                // 显示Search Everywhere对话框并尝试切换到我们的标签页
                manager.show("RestfulEndpoints", "", e);
                return;
            }
        } catch (Exception ex) {
            // 如果直接切换失败，回退到原来的方式
        }

        // 回退方案：触发系统的"Search Everywhere"功能
        ActionManager actionManager = ActionManager.getInstance();
        AnAction searchEverywhereAction = actionManager.getAction("SearchEverywhere");
        if (searchEverywhereAction != null) {
            searchEverywhereAction.actionPerformed(e);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 确保在有项目的情况下才启用此动作
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}