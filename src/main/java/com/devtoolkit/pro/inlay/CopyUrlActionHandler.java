package com.devtoolkit.pro.inlay;

import com.intellij.codeInsight.hints.declarative.InlayActionHandler;
import com.intellij.codeInsight.hints.declarative.InlayActionPayload;
import com.intellij.codeInsight.hints.declarative.StringInlayActionPayload;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;

/**
 * 处理复制URL到剪贴板的Action Handler
 */
public class CopyUrlActionHandler implements InlayActionHandler {
    
    public static final String HANDLER_ID = "copy.url.action";
    
    @Override
    public void handleClick(@NotNull Editor editor, 
                           @NotNull InlayActionPayload payload) {
        if (payload instanceof StringInlayActionPayload) {
            StringInlayActionPayload stringPayload = (StringInlayActionPayload) payload;
            String url = stringPayload.getText();
            
            // 复制URL到剪贴板
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            
            // 可以添加通知提示用户复制成功
            // Project project = editor.getProject();
            // if (project != null) {
            //     NotificationGroupManager.getInstance()
            //         .getNotificationGroup("DevToolkitPro")
            //         .createNotification("URL已复制到剪贴板: " + url, NotificationType.INFORMATION)
            //         .notify(project);
            // }
        }
    }
}