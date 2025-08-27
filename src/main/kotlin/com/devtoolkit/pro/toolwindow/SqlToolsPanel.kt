package com.devtoolkit.pro.toolwindow

import com.devtoolkit.pro.services.SqlFormatterService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
                    val formattedSql = sqlFormatterService.formatSql(sql)
                    sqlEditor.text = formattedSql
                    copyToClipboard(formattedSql)
                    Messages.showInfoMessage(project, "格式化的SQL已复制到剪贴板", "DevToolkitPro")
                } catch (e: Exception) {
                    Messages.showErrorDialog(project, "格式化失败: ${e.message}", "DevToolkitPro")
                }
            } else {
                Messages.showWarningDialog(project, "请输入SQL语句", "DevToolkitPro")
            }
        }
        
        compressButton.addActionListener {
            val sql = sqlEditor.text.trim()
            if (sql.isNotEmpty()) {
                try {
                    val compressedSql = sqlFormatterService.compressSql(sql)
                    sqlEditor.text = compressedSql
                    copyToClipboard(compressedSql)
                    Messages.showInfoMessage(project, "压缩的SQL已复制到剪贴板", "DevToolkitPro")
                } catch (e: Exception) {
                    Messages.showErrorDialog(project, "压缩失败: ${e.message}", "DevToolkitPro")
                }
            } else {
                Messages.showWarningDialog(project, "请输入SQL语句", "DevToolkitPro")
            }
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
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