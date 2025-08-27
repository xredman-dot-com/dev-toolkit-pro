package com.devtoolkit.pro.toolwindow

import com.devtoolkit.pro.swagger.ApiDocumentGenerator
import com.devtoolkit.pro.swagger.ProgressCallback
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.Desktop
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*

/**
 * API文档生成面板
 */
class ApiDocPanel(private val project: Project) {
    
    private val documentNameField = JBTextField()
    private val swaggerUrlField = JBTextField()
    private val outputPathField = TextFieldWithBrowseButton()
    private val generateButton = JButton("生成接口文档")
    private val panel = JPanel(BorderLayout())
    
    init {
        initializeComponents()
        setupEventHandlers()
    }
    
    private fun initializeComponents() {
        // 设置默认值
        documentNameField.text = "API接口文档"
        swaggerUrlField.text = ""
        
        // 设置输出路径选择器
        outputPathField.addBrowseFolderListener(
            "选择输出目录",
            "请选择API文档的输出目录",
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        
        // 设置默认输出路径为用户下载目录
        val defaultOutputPath = System.getProperty("user.home") + "/Downloads"
        outputPathField.text = defaultOutputPath
        
        // 构建表单
        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("文档名称:"), documentNameField, 1, false)
            .addLabeledComponent(JBLabel("Swagger地址:"), swaggerUrlField, 1, false)
            .addLabeledComponent(JBLabel("输出路径:"), outputPathField, 1, false)
            .addComponent(generateButton, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.add(formPanel, BorderLayout.CENTER)
    }
    
    private fun setupEventHandlers() {
        generateButton.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                generateApiDocument()
            }
        })
    }
    
    private fun generateApiDocument() {
        val documentName = documentNameField.text.trim()
        val swaggerUrl = swaggerUrlField.text.trim()
        val outputPath = outputPathField.text.trim()
        
        // 验证输入
        if (documentName.isEmpty()) {
            Messages.showErrorDialog("请输入文档名称", "输入错误")
            return
        }
        
        if (swaggerUrl.isEmpty()) {
            Messages.showErrorDialog("请输入Swagger地址", "输入错误")
            return
        }
        
        if (outputPath.isEmpty()) {
            Messages.showErrorDialog("请选择输出路径", "输入错误")
            return
        }
        
        // 确保输出目录存在
        val outputDir = File(outputPath)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        // 构建输出文件路径
        val outputFile = File(outputDir, "$documentName.docx")
        
        // 显示进度提示
        generateButton.isEnabled = false
        generateButton.text = "生成中..."
        
        // 使用后台任务执行网络请求和文档生成
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "生成API文档", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // 设置进度指示器为确定模式
                    indicator.isIndeterminate = false
                    
                    indicator.text = "正在获取Swagger数据..."
                    indicator.fraction = 0.1
                    
                    val generator = ApiDocumentGenerator()
                    var totalApiCount = 0
                    
                    // 创建进度回调
                    val progressCallback = object : ProgressCallback {
                        override fun onProgress(current: Int, total: Int, currentApi: String) {
                            totalApiCount = total
                            val progress = 0.1 + (current.toDouble() / total.toDouble()) * 0.8
                            indicator.fraction = progress
                            indicator.text = "正在生成接口文档 ($current/$total): $currentApi"
                        }
                    }
                    
                    generator.generate(documentName, swaggerUrl, outputFile.absolutePath, progressCallback)
                    
                    indicator.fraction = 1.0
                    indicator.text = "文档生成完成"
                    
                    // 在EDT线程中更新UI
                    ApplicationManager.getApplication().invokeLater {
                        generateButton.isEnabled = true
                        generateButton.text = "生成接口文档"
                        
                        // 显示生成总结对话框
                        showGenerationSummary(outputFile, totalApiCount)
                    }
                } catch (ex: Exception) {
                    // 在EDT线程中更新UI
                    ApplicationManager.getApplication().invokeLater {
                        generateButton.isEnabled = true
                        generateButton.text = "生成接口文档"
                        
                        Messages.showErrorDialog(
                            "生成失败: ${ex.message}",
                            "生成失败"
                        )
                    }
                }
            }
        })
    }
    
    fun getContent(): JComponent {
        return panel
    }
    
    /**
     * 显示生成总结对话框
     */
    private fun showGenerationSummary(outputFile: File, apiCount: Int) {
        val message = "API文档生成成功！\n\n" +
                "生成的API数量：$apiCount 个\n" +
                "文档保存路径：${outputFile.absolutePath}\n\n" +
                "是否要打开生成的文档？"
        
        val result = Messages.showYesNoDialog(
            message,
            "生成完成",
            "打开文档",
            "取消",
            Messages.getInformationIcon()
        )
        
        if (result == Messages.YES) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(outputFile)
                } else {
                    Messages.showInfoMessage(
                        "系统不支持自动打开文档，请手动打开：\n${outputFile.absolutePath}",
                        "提示"
                    )
                }
            } catch (ex: Exception) {
                Messages.showErrorDialog(
                    "无法打开文档：${ex.message}\n\n文档路径：${outputFile.absolutePath}",
                    "打开失败"
                )
            }
        }
    }
}