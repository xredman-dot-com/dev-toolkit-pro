package com.devtoolkit.pro.database.actions

import com.devtoolkit.pro.database.DatabaseMetadataExtractor
import com.devtoolkit.pro.database.ExcelDocumentBuilder
import com.devtoolkit.pro.database.dialogs.DatabaseDocConfigDialog
import com.intellij.database.model.DasDataSource
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbPsiFacade
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import java.io.File

/**
 * 生成数据库文档的Action
 */
class GenerateDatabaseDocAction : AnAction("生成数据库说明文档") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 显示配置对话框
        val configDialog = DatabaseDocConfigDialog(project)
        if (!configDialog.showAndGet()) {
            return
        }
        
        val config = configDialog.getConfig()
        
        // 在后台任务中生成文档
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "生成数据库文档", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "正在检查数据库连接..."
                    indicator.fraction = 0.05
                    
                    // 在后台线程中获取数据源
                    val dataSources = DbPsiFacade.getInstance(project).dataSources
                    if (dataSources.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(
                                project,
                                "未找到数据库连接，请先配置数据源",
                                "生成数据库文档"
                            )
                        }
                        return
                    }
                    
                    indicator.text = "正在提取数据库元数据..."
                    indicator.fraction = 0.1
                    
                    val extractor = DatabaseMetadataExtractor()
                    // 从第一个数据源提取所有表信息
                    val tables = extractor.extractTablesFromDataSource(dataSources.first().delegate)
                    
                    indicator.text = "正在生成Excel文档..."
                    indicator.fraction = 0.5
                    
                    val builder = ExcelDocumentBuilder()
                    val outputFile = File(config.outputPath)
                    builder.generateDocument(config.title, tables, outputFile)
                    
                    indicator.text = "文档生成完成"
                    indicator.fraction = 1.0
                    
                    // 在EDT线程中显示成功消息
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "数据库文档已生成：${config.outputPath}",
                            "生成成功"
                        )
                    }
                    
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "生成文档时发生错误：${e.message}",
                            "生成失败"
                        )
                    }
                }
            }
        })
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        
        // 简化update方法，避免在EDT线程上进行文件系统操作
        // 只要项目存在就显示action，具体的数据源检查在actionPerformed中进行
        e.presentation.isEnabledAndVisible = true
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}