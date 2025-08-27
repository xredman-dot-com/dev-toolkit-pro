package com.devtoolkit.pro.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * DevToolkitPro工具窗口工厂类
 * 负责创建和初始化工具窗口
 */
class DevToolkitProToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val devToolkitProToolWindow = DevToolkitProToolWindow(project)
        val content = ContentFactory.getInstance().createContent(
            devToolkitProToolWindow.getContent(), 
            "", 
            false
        )
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean = true
}