package com.devtoolkit.pro.actions;

import com.devtoolkit.pro.ui.SearchDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 全局RESTful URL搜索动作
 * 通过快捷键触发模糊搜索对话框
 */
public class SearchRestfulUrlsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 创建并显示搜索对话框
        SearchDialog dialog = new SearchDialog(project);
        dialog.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 确保在有项目的情况下才启用此动作
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}