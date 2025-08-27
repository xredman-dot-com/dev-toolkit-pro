package com.devtoolkit.pro.actions;

import com.devtoolkit.pro.utils.GitLinkUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;

/**
 * 编辑器Git链接复制Action
 * 在编辑器右键菜单中提供复制GitHub/GitLab链接的功能
 */
public class CopyEditorGitLinkAction extends AnAction {

    public CopyEditorGitLinkAction() {
        super("Copy Git Link", "Copy GitHub/GitLab link for current file/line",
              com.intellij.icons.AllIcons.Vcs.Vendors.Github);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || editor == null || virtualFile == null) {
            Messages.showErrorDialog(project, "无法获取当前文件信息", "错误");
            return;
        }

        if (!GitLinkUtil.isGitRepository(project)) {
            Messages.showErrorDialog(project, "当前项目不是Git仓库", "Git链接不可用");
            return;
        }

        // 获取Git仓库信息
        GitLinkUtil.GitRepoInfo repoInfo = GitLinkUtil.getGitRepoInfo(project, virtualFile);
        if (repoInfo == null) {
            Messages.showErrorDialog(project, "无法获取Git仓库信息", "错误");
            return;
        }

        // 显示选择菜单
        showGitLinkPopup(e, project, editor, virtualFile, repoInfo);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        boolean enabled = project != null && editor != null && virtualFile != null
                         && GitLinkUtil.isGitRepository(project);

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);

        // 如果可用，尝试更新显示文本以包含Git平台信息
        if (enabled && virtualFile != null) {
            try {
                GitLinkUtil.GitRepoInfo repoInfo = GitLinkUtil.getGitRepoInfo(project, virtualFile);
                if (repoInfo != null) {
                    String platformName = repoInfo.getPlatform().getDisplayName();
                    e.getPresentation().setText("Copy " + platformName + " Link");
                    e.getPresentation().setDescription("Copy " + platformName + " link for current file/line");
                }
            } catch (Exception ex) {
                // 忽略错误，使用默认文本
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 显示Git链接选择弹出菜单
     */
    private void showGitLinkPopup(@NotNull AnActionEvent e, @NotNull Project project,
                                 @NotNull Editor editor, @NotNull VirtualFile virtualFile,
                                 @NotNull GitLinkUtil.GitRepoInfo repoInfo) {

        String relativePath = GitLinkUtil.getRelativePath(project, virtualFile);
        if (relativePath == null) {
            Messages.showErrorDialog(project, "无法获取文件相对路径", "错误");
            return;
        }

        List<GitLinkOption> options = Arrays.asList(
            new GitLinkOption("📄 Copy File Link", "复制文件链接", () -> {
                copyFileLink(project, repoInfo, relativePath);
            }),
            new GitLinkOption("🎯 Copy Current Line Link", "复制当前行链接", () -> {
                copyCurrentLineLink(project, editor, repoInfo, relativePath);
            }),
            new GitLinkOption("📋 Copy Selected Lines Link", "复制选中行范围链接", () -> {
                copySelectedLinesLink(project, editor, repoInfo, relativePath);
            })
        );

        BaseListPopupStep<GitLinkOption> step = new BaseListPopupStep<GitLinkOption>(
            repoInfo.getPlatform().getDisplayName() + " Links", options) {

            @Override
            public PopupStep<?> onChosen(GitLinkOption selectedValue, boolean finalChoice) {
                if (finalChoice) {
                    selectedValue.action.run();
                }
                return FINAL_CHOICE;
            }

            @NotNull
            @Override
            public String getTextFor(GitLinkOption value) {
                return value.text;
            }

            public String getTooltipTextFor(GitLinkOption value) {
                return value.description;
            }

            @Override
            public Icon getIconFor(GitLinkOption value) {
                if (value.text.contains("File")) {
                    return com.intellij.icons.AllIcons.FileTypes.Any_type;
                } else if (value.text.contains("Line")) {
                    return com.intellij.icons.AllIcons.General.ArrowRight;
                } else {
                    return com.intellij.icons.AllIcons.Actions.Copy;
                }
            }
        };

        JBPopupFactory.getInstance()
            .createListPopup(step)
            .showInBestPositionFor(e.getDataContext());
    }

    /**
     * 复制文件链接
     */
    private void copyFileLink(Project project, GitLinkUtil.GitRepoInfo repoInfo, String relativePath) {
        String url = GitLinkUtil.generateFileUrl(repoInfo, relativePath);
        if (url != null) {
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            Messages.showInfoMessage(project,
                "文件链接已复制到剪贴板\n" + url,
                "Git链接复制成功");
        } else {
            Messages.showErrorDialog(project, "无法生成文件链接", "错误");
        }
    }

    /**
     * 复制当前行链接
     */
    private void copyCurrentLineLink(Project project, Editor editor,
                                   GitLinkUtil.GitRepoInfo repoInfo, String relativePath) {
        int lineNumber = GitLinkUtil.getCurrentLineNumber(editor);
        String url = GitLinkUtil.generateFileUrlWithLine(repoInfo, relativePath, lineNumber);
        if (url != null) {
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            Messages.showInfoMessage(project,
                String.format("%s 链接已复制到剪贴板\n%s\n\n定位到第 %d 行",
                    repoInfo.getPlatform().getDisplayName(), url, lineNumber),
                "Git链接复制成功");
        } else {
            Messages.showErrorDialog(project, "无法生成行链接", "错误");
        }
    }

    /**
     * 复制选中行范围链接
     */
    private void copySelectedLinesLink(Project project, Editor editor,
                                     GitLinkUtil.GitRepoInfo repoInfo, String relativePath) {
        int[] lineRange = GitLinkUtil.getSelectedLineRange(editor);
        int startLine = lineRange[0];
        int endLine = lineRange[1];

        String url;
        if (startLine == endLine) {
            url = GitLinkUtil.generateFileUrlWithLine(repoInfo, relativePath, startLine);
        } else {
            url = GitLinkUtil.generateFileUrlWithLineRange(repoInfo, relativePath, startLine, endLine);
        }

        if (url != null) {
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            String message;
            if (startLine == endLine) {
                message = String.format("%s 链接已复制到剪贴板\n%s\n\n定位到第 %d 行",
                    repoInfo.getPlatform().getDisplayName(), url, startLine);
            } else {
                message = String.format("%s 链接已复制到剪贴板\n%s\n\n定位到第 %d-%d 行",
                    repoInfo.getPlatform().getDisplayName(), url, startLine, endLine);
            }
            Messages.showInfoMessage(project, message, "Git链接复制成功");
        } else {
            Messages.showErrorDialog(project, "无法生成行范围链接", "错误");
        }
    }

    /**
     * Git链接选项类
     */
    private static class GitLinkOption {
        final String text;
        final String description;
        final Runnable action;

        GitLinkOption(String text, String description, Runnable action) {
            this.text = text;
            this.description = description;
            this.action = action;
        }
    }
}