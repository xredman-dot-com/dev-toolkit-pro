package com.devtoolkit.pro.swagger

import com.devtoolkit.pro.document.WordDocumentBuilder
import com.devtoolkit.pro.swagger.model.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 进度回调接口
 */
interface ProgressCallback {
    fun onProgress(current: Int, total: Int, currentApi: String)
}

/**
 * API文档生成器
 */
class ApiDocumentGenerator {
    
    /**
     * 生成API文档
     */
    fun generate(projectName: String, swaggerUrl: String, outputPath: String, progressCallback: ProgressCallback? = null) {
        val parser = SwaggerParser()
        val apiGroups = parser.parse(swaggerUrl)
        
        val outputFile = File(outputPath)
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        
        buildApiDocs(projectName, apiGroups, outputFile, progressCallback)
    }
    
    /**
     * 构建API文档
     */
    private fun buildApiDocs(projectName: String, apiGroups: Map<String, List<SwaggerApi>>, outputFile: File, progressCallback: ProgressCallback? = null) {
        val builder = WordDocumentBuilder.newBuilder()
        
        // 设置页眉和标题
        builder.header("$projectName API接口文档")
            .title("$projectName API接口文档", "版本：1.0")
        
        // 添加文档信息
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        builder.h1("文档信息")
            .table(3, 2)
            .kv("文档名称", "$projectName API接口文档")
            .newRow()
            .kv("生成时间", currentTime)
            .newRow()
            .kv("文档版本", "1.0")
            .end()
        
        // 计算总API数量
        val totalApis = apiGroups.values.sumOf { it.size }
        var currentApiIndex = 0
        
        // 遍历API分组
        apiGroups.forEach { (groupName, apis) ->
            builder.h1(groupName)
            
            apis.forEach { api ->
                currentApiIndex++
                progressCallback?.onProgress(currentApiIndex, totalApis, api.summary.ifEmpty { "未命名接口" })
                buildApi(builder, api)
            }
        }
        
        builder.build(outputFile)
    }
    
    /**
     * 构建单个API
     */
    private fun buildApi(builder: WordDocumentBuilder, api: SwaggerApi) {
        // API基本信息
        builder.h2(api.summary.ifEmpty { "未命名接口" })
        
        // 计算表格行数：基本信息5行 + 参数表头1行 + 参数行数 + 响应表头1行 + 响应行数
        val paramRows = if (api.parameters.isNotEmpty()) api.parameters.size + 1 else 0
        val responseRows = if (api.responses.isNotEmpty()) api.responses.size + 1 else 0
        val totalRows = 5 + paramRows + responseRows
        
        val tableBuilder = builder.table(totalRows, 6)
            // 设置列宽度：标题列(1200), 内容列(2000), 标题列(1000), 内容列(2000), 描述列(2500), 示例列(1300)
            .setColumnWidths(1200, 2000, 1000, 2000, 2500, 1300)
        
        // API基本信息部分
        tableBuilder.mergedCell("接口路径", 1, true)
            .mergedCell(api.path, 5, false)
            .newRow()
            .hCell("请求方法")
            .mergedCell(api.method.uppercase(), 2, false)
            .hCell("操作标识")
            .mergedCell(api.operationId, 2, false)
            .newRow()
            .hCell("请求类型")
            .mergedCell(api.consumes.joinToString(", "), 2, false)
            .hCell("响应类型")
            .mergedCell(api.produces.joinToString(", "), 2, false)
            .newRow()
            .mergedCell("接口说明", 1, true)
            .mergedCell(api.description.ifEmpty { api.summary }, 5, false)
        
        // 请求参数部分
        if (api.parameters.isNotEmpty()) {
            tableBuilder.newRow()
                .mergedCell("请求参数", 6, true)
                .newRow()
                .hCell("参数名")
                .hCell("位置")
                .hCell("数据类型")
                .hCell("必填")
                .hCell("说明")
                .hCell("示例")
            
            api.parameters.forEach { param ->
                tableBuilder.newRow()
                    .cell(param.name)
                    .cell(param.`in`)
                    .cell(getParameterType(param))
                    .cell(if (param.required) "是" else "否")
                    .cell(param.description)
                    .cell(getParameterExample(param))
            }
        }
        
        // 响应结果部分
        if (api.responses.isNotEmpty()) {
            tableBuilder.newRow()
                .mergedCell("响应信息", 6, true)
                .newRow()
                .hCell("状态码")
                .hCell("说明")
                .mergedCell("数据结构", 4, true)
            
            api.responses.forEach { response ->
                tableBuilder.newRow()
                    .cell(response.code.toString())
                    .cell(response.description)
                    .mergedCell(response.refDefinition?.title ?: "", 4, false)
            }
        }
        
        tableBuilder.end()
    }
    

    
    /**
     * 获取参数类型
     */
    private fun getParameterType(param: Parameter): String {
        return when {
            param.refDefinition != null -> param.refDefinition!!.title.ifEmpty { "object" }
            param.type.isNotEmpty() -> {
                if (param.type == "array" && param.items != null) {
                    "array[${param.items!!.type.ifEmpty { "object" }}]"
                } else {
                    param.type
                }
            }
            else -> "string"
        }
    }
    
    /**
     * 获取参数示例
     */
    private fun getParameterExample(param: Parameter): String {
        return when (param.type) {
            "string" -> "string"
            "integer" -> "0"
            "number" -> "0.0"
            "boolean" -> "true"
            "array" -> "[]"
            else -> "{}"
        }
    }
    
    /**
     * 获取属性类型
     */
    private fun getPropertyType(prop: DefinitionProperty): String {
        return when {
            prop.refDefinition != null -> prop.refDefinition!!.title.ifEmpty { "object" }
            prop.type.isNotEmpty() -> {
                if (prop.type == "array" && prop.items != null) {
                    "array[${prop.items!!.type.ifEmpty { "object" }}]"
                } else {
                    prop.type
                }
            }
            else -> "string"
        }
    }
    
    /**
     * 获取属性示例
     */
    private fun getPropertyExample(prop: DefinitionProperty): String {
        return when (prop.type) {
            "string" -> "string"
            "integer" -> "0"
            "number" -> "0.0"
            "boolean" -> "true"
            "array" -> "[]"
            else -> "{}"
        }
    }
}