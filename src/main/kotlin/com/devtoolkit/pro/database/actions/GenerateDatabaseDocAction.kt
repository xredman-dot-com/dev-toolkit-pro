package com.devtoolkit.pro.database.actions

import com.devtoolkit.pro.database.DatabaseMetadataExtractor
import com.devtoolkit.pro.database.ExcelDocumentBuilder
import com.devtoolkit.pro.database.dialogs.DatabaseDocConfigDialog
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File

/**
 * 生成数据库文档的Action
 */
class GenerateDatabaseDocAction : AnAction("生成数据库说明文档") {
    
    companion object {
        private const val DATABASE_PLUGIN_ID = "com.intellij.database"
    }
    
    /**
     * 检查数据库插件是否可用
     */
    private fun isDatabasePluginAvailable(): Boolean {
        return try {
            val pluginId = PluginId.getId(DATABASE_PLUGIN_ID)
            PluginManagerCore.isPluginInstalled(pluginId) && 
            PluginManagerCore.getPlugin(pluginId)?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 检查数据库插件是否可用
        if (!isDatabasePluginAvailable()) {
            Messages.showWarningDialog(
                project,
                "此功能需要安装并启用 Database Tools and SQL 插件。\n请在 Settings > Plugins 中安装该插件后重试。",
                "数据库插件不可用"
            )
            return
        }
        
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
                    
                    // 使用反射安全地访问数据库API
                    val dataSources = getDataSourcesSafely(project)
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
                    val tables = extractor.extractTablesFromDataSource(dataSources.first())
                    
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
    
    /**
     * 使用反射安全地获取数据源列表
     */
    private fun getDataSourcesSafely(project: Project): List<Any> {
        return try {
            val dbPsiFacadeClass = Class.forName("com.intellij.database.psi.DbPsiFacade")
            val getInstanceMethod = dbPsiFacadeClass.getMethod("getInstance", Project::class.java)
            val dbPsiFacade = getInstanceMethod.invoke(null, project)
            
            val getDataSourcesMethod = dbPsiFacadeClass.getMethod("getDataSources")
            val dataSources = getDataSourcesMethod.invoke(dbPsiFacade) as Collection<*>
            
            dataSources.mapNotNull { dataSource ->
                // 获取delegate属性
                val delegateField = dataSource?.javaClass?.getField("delegate")
                delegateField?.get(dataSource)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        
        // 检查数据库插件是否可用
        e.presentation.isEnabledAndVisible = isDatabasePluginAvailable()
        
        if (!isDatabasePluginAvailable()) {
            e.presentation.description = "需要安装并启用 Database Tools and SQL 插件"
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}