package com.devtoolkit.pro.actions;

import com.devtoolkit.pro.utils.GitLinkUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;

/**
 * 复制Git链接Action
 * 直接复制当前行的Git链接到剪贴板
 */
public class CopyGitLinkAction extends AnAction {

    public CopyGitLinkAction() {
        super("Copy Git Link", "Copy Git link for current line", loadPluginIcon());
    }

    private static Icon loadPluginIcon() {
        try {
            return new ImageIcon(CopyGitLinkAction.class.getResource("/icons/plugin-icon-16.png"));
        } catch (Exception e) {
            return com.intellij.icons.AllIcons.Vcs.Vendors.Github;
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || editor == null || virtualFile == null) {
            return;
        }

        // 获取Git仓库信息
        GitLinkUtil.GitRepoInfo repoInfo = GitLinkUtil.getGitRepoInfo(project, virtualFile);
        if (repoInfo == null) {
            return;
        }

        String relativePath = GitLinkUtil.getRelativePath(project, virtualFile);
        if (relativePath == null) {
            return;
        }

        // 生成当前行链接
        int lineNumber = GitLinkUtil.getCurrentLineNumber(editor);
        String url = GitLinkUtil.generateFileUrlWithLine(repoInfo, relativePath, lineNumber);
        
        if (url != null) {
            // 复制到剪贴板
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            
            // 显示成功通知
            Notification notification = new Notification(
                "DevToolkit Pro",
                "Git链接复制成功",
                String.format("已复制第 %d 行的Git链接到剪贴板", lineNumber),
                NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, project);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        boolean enabled = project != null && editor != null && virtualFile != null
                         && GitLinkUtil.isGitRepository(project)
                         && hasRemoteRepository(project, virtualFile);

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 检查是否有远程仓库
     */
    private boolean hasRemoteRepository(Project project, VirtualFile virtualFile) {
        try {
            GitLinkUtil.GitRepoInfo repoInfo = GitLinkUtil.getGitRepoInfo(project, virtualFile);
            return repoInfo != null;
        } catch (Exception e) {
            return false;
        }
    }
}