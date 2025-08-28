package com.devtoolkit.pro.database.dialogs

import com.devtoolkit.pro.database.model.DatabaseDocConfig
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 数据库文档生成配置对话框
 */
class DatabaseDocConfigDialog(private val project: Project) : DialogWrapper(project) {
    
    private val titleField = JBTextField()
    private val outputPathField = TextFieldWithBrowseButton()
    
    init {
        title = "生成数据库说明文档"
        init()
        initializeFields()
    }
    
    private fun initializeFields() {
        // 设置默认标题
        titleField.text = "数据库表结构设计文档"
        
        // 设置默认输出路径为用户下载目录
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        val defaultFileName = "数据库表结构文档_${System.currentTimeMillis()}.xlsx"
        val defaultPath = File(downloadsDir, defaultFileName).absolutePath
        outputPathField.text = defaultPath
        
        // 配置文件选择器
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xlsx")
        descriptor.title = "选择输出文件"
        descriptor.description = "选择Excel文档的保存位置"
        outputPathField.addBrowseFolderListener(
            "选择输出文件",
            "选择Excel文档的保存位置",
            project,
            descriptor
        )
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("文档标题:"), titleField, 1, false)
            .addLabeledComponent(JBLabel("输出路径:"), outputPathField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.preferredSize = Dimension(500, 150)
        return panel
    }
    
    override fun doValidate(): ValidationInfo? {
        if (titleField.text.isBlank()) {
            return ValidationInfo("请输入文档标题", titleField)
        }
        
        if (outputPathField.text.isBlank()) {
            return ValidationInfo("请选择输出路径", outputPathField)
        }
        
        val outputFile = File(outputPathField.text)
        val parentDir = outputFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            return ValidationInfo("输出目录不存在", outputPathField)
        }
        
        if (!outputPathField.text.endsWith(".xlsx", ignoreCase = true)) {
            return ValidationInfo("输出文件必须是Excel格式(.xlsx)", outputPathField)
        }
        
        return null
    }
    
    /**
     * 获取配置信息
     */
    fun getConfig(): DatabaseDocConfig {
        return DatabaseDocConfig(
            title = titleField.text.trim(),
            outputPath = outputPathField.text.trim()
        )
    }
}