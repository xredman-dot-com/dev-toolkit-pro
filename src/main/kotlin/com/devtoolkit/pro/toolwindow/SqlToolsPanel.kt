package com.devtoolkit.pro.toolwindow

import com.devtoolkit.pro.services.SqlFormatterService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.EditorTextField
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.editor.EditorSettings
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*

/**
 * SQL工具面板
 * 提供SQL格式化和压缩功能
 */
class SqlToolsPanel(private val project: Project) {
    
    private val sqlEditor: EditorTextField
    private val formatButton = JButton("格式化SQL")
    private val compressButton = JButton("压缩SQL")
    private val sqlFormatterService = SqlFormatterService()
    
    init {
        // 创建SQL编辑器，支持语法高亮
        val sqlFileType = FileTypeManager.getInstance().getFileTypeByExtension("sql")
        sqlEditor = EditorTextField("", project, sqlFileType)
        initializeComponents()
        setupEventListeners()
    }
    
    private fun initializeComponents() {
        // 设置SQL编辑器
        sqlEditor.setOneLineMode(false)
        sqlEditor.preferredSize = java.awt.Dimension(400, 300)
        sqlEditor.toolTipText = "在此处粘贴或输入SQL语句"
        
        // 配置编辑器设置
        sqlEditor.addSettingsProvider { editor ->
            val settings = editor.settings
            settings.isLineNumbersShown = true
            settings.isAutoCodeFoldingEnabled = true
            settings.isFoldingOutlineShown = true
            settings.isAllowSingleLogicalLineFolding = true
        }
        
        // 设置按钮
        formatButton.toolTipText = "格式化SQL并更新文本框内容，同时复制到剪贴板"
        compressButton.toolTipText = "压缩SQL为一行并更新文本框内容，同时复制到剪贴板"
    }
    
    private fun setupEventListeners() {
        formatButton.addActionListener {
            val sql = sqlEditor.text.trim()
            if (sql.isNotEmpty()) {
                try {
                    val cleanedSql = removeCommentsAndEmptyLines(sql)
                    val formattedSql = sqlFormatterService.formatSql(cleanedSql)
                    sqlEditor.text = formattedSql
                    copyToClipboard(formattedSql)
                    showNotification("格式化的SQL已复制到剪贴板", NotificationType.INFORMATION)
                } catch (e: Exception) {
                    showNotification("格式化失败: ${e.message}", NotificationType.ERROR)
                }
            } else {
                showNotification("请输入SQL语句", NotificationType.WARNING)
            }
        }
        
        compressButton.addActionListener {
            val sql = sqlEditor.text.trim()
            if (sql.isNotEmpty()) {
                try {
                    val cleanedSql = removeCommentsAndEmptyLines(sql)
                    val compressedSql = sqlFormatterService.compressSql(cleanedSql)
                    sqlEditor.text = compressedSql
                    copyToClipboard(compressedSql)
                    showNotification("压缩的SQL已复制到剪贴板", NotificationType.INFORMATION)
                } catch (e: Exception) {
                    showNotification("压缩失败: ${e.message}", NotificationType.ERROR)
                }
            } else {
                showNotification("请输入SQL语句", NotificationType.WARNING)
            }
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    }
    
    private fun removeCommentsAndEmptyLines(sql: String): String {
        return sql.lines()
            .map { line ->
                // 移除单行注释 (-- 注释)
                val commentIndex = line.indexOf("--")
                if (commentIndex != -1) {
                    line.substring(0, commentIndex)
                } else {
                    line
                }
            }
            .map { line ->
                // 移除多行注释 (/* */ 注释)
                var result = line
                var startIndex = result.indexOf("/*")
                while (startIndex != -1) {
                    val endIndex = result.indexOf("*/", startIndex + 2)
                    if (endIndex != -1) {
                        result = result.substring(0, startIndex) + result.substring(endIndex + 2)
                        startIndex = result.indexOf("/*")
                    } else {
                        result = result.substring(0, startIndex)
                        break
                    }
                }
                result
            }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
    
    private fun showNotification(message: String, type: NotificationType) {
        val notification = Notification(
            "DevToolkitPro",
            "SQL工具",
            message,
            type
        )
        Notifications.Bus.notify(notification, project)
    }

    
    fun getContent(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)
        
        // 创建编辑器区域
        val editorPanel = JPanel(BorderLayout())
        editorPanel.add(sqlEditor, BorderLayout.CENTER)
        editorPanel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
            JBUI.Borders.empty(5)
        )
        
        // 创建按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(formatButton)
        buttonPanel.add(compressButton)
        
        // 组装面板
        panel.add(editorPanel, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        return panel
    }
}