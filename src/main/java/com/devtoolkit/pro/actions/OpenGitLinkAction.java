package com.devtoolkit.pro.actions;

import com.devtoolkit.pro.utils.GitLinkUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 打开Git链接Action
 * 在系统默认浏览器中打开当前行的Git链接
 */
public class OpenGitLinkAction extends AnAction {

    public OpenGitLinkAction() {
        super("Open Git Link", "Open Git link in browser for current line", loadPluginIcon());
    }

    private static Icon loadPluginIcon() {
        try {
            return new ImageIcon(OpenGitLinkAction.class.getResource("/icons/plugin-icon-16.png"));
        } catch (Exception e) {
            return com.intellij.icons.AllIcons.Ide.External_link_arrow;
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
            // 在浏览器中打开链接
            BrowserUtil.browse(url);
            
            // 显示成功通知
            Notification notification = new Notification(
                "DevToolkit Pro",
                "Git链接已打开",
                String.format("已在浏览器中打开第 %d 行的Git链接", lineNumber),
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