package com.devtoolkit.pro.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import javax.swing.JComponent

/**
 * DevToolkitPro主工具窗口类
 * 包含多个功能标签页
 */
class DevToolkitProToolWindow(private val project: Project) {
    
    private val tabbedPane = JBTabbedPane()
    
    init {
        initializeComponents()
    }
    
    private fun initializeComponents() {
        // 添加SQL Tools标签页
        val sqlToolsPanel = SqlToolsPanel(project)
        tabbedPane.addTab("SQL Tools", sqlToolsPanel.getContent())
        
        // 添加接口文档标签页
        val apiDocPanel = ApiDocPanel(project)
        tabbedPane.addTab("接口文档", apiDocPanel.getContent())
        
        // 可以在这里添加更多标签页
        // tabbedPane.addTab("Other Tools", otherPanel.getContent())
    }
    
    fun getContent(): JComponent {
        return tabbedPane
    }
}