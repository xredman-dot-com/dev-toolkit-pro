package com.devtoolkit.pro.swagger

import com.devtoolkit.pro.document.WordDocumentBuilder
import com.devtoolkit.pro.swagger.model.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * API文档生成器
 */
class ApiDocumentGenerator {
    
    /**
     * 生成API文档
     */
    fun generate(projectName: String, swaggerUrl: String, outputPath: String) {
        val parser = SwaggerParser()
        val apiGroups = parser.parse(swaggerUrl)
        
        val outputFile = File(outputPath)
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        
        buildApiDocs(projectName, apiGroups, outputFile)
    }
    
    /**
     * 构建API文档
     */
    private fun buildApiDocs(projectName: String, apiGroups: Map<String, List<SwaggerApi>>, outputFile: File) {
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
        
        // 遍历API分组
        apiGroups.forEach { (groupName, apis) ->
            builder.h1(groupName)
            
            apis.forEach { api ->
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
        builder.h2("${api.summary.ifEmpty { "未命名接口" }} (${api.method.uppercase()} ${api.path})")
        
        val tableBuilder = builder.table(6, 2)
        tableBuilder.kv("接口地址", api.path)
            .newRow()
            .kv("请求方式", api.method.uppercase())
            .newRow()
            .kv("接口描述", api.description.ifEmpty { api.summary })
            .newRow()
            .kv("操作ID", api.operationId)
            .newRow()
            .kv("消费类型", api.consumes.joinToString(", "))
            .newRow()
            .kv("生产类型", api.produces.joinToString(", "))
            .end()
        
        // 请求参数
        if (api.parameters.isNotEmpty()) {
            builder.h3("请求参数")
            buildApiParameters(builder, api.parameters)
        }
        
        // 响应结果
        if (api.responses.isNotEmpty()) {
            builder.h3("响应结果")
            buildApiResponses(builder, api.responses)
        }
    }
    
    /**
     * 构建API参数表格
     */
    private fun buildApiParameters(builder: WordDocumentBuilder, parameters: List<Parameter>) {
        val tableBuilder = builder.table(parameters.size + 1, 6)
        
        // 表头
        tableBuilder.hCell("参数名称")
            .hCell("参数位置")
            .hCell("参数类型")
            .hCell("是否必需")
            .hCell("参数描述")
            .hCell("示例值")
        
        // 参数行
        parameters.forEach { param: Parameter ->
            tableBuilder.newRow()
                .cell(param.name)
                .cell(param.`in`)
                .cell(getParameterType(param))
                .cell(if (param.required) "是" else "否")
                .cell(param.description)
                .cell(getParameterExample(param))
        }
        
        tableBuilder.end()
    }
    
    /**
     * 构建API响应表格
     */
    private fun buildApiResponses(builder: WordDocumentBuilder, responses: List<Response>) {
        val tableBuilder = builder.table(responses.size + 1, 3)
        
        // 表头
        tableBuilder.hCell("状态码")
            .hCell("描述")
            .hCell("数据结构")
        
        // 响应行
        responses.forEach { response: Response ->
            tableBuilder.newRow()
                .cell(response.code.toString())
                .cell(response.description)
                .cell(response.refDefinition?.title ?: "")
        }
        
        tableBuilder.end()
        
        // 详细数据结构
        responses.forEach { response: Response ->
            response.refDefinition?.let { definition ->
                if (definition.properties().isNotEmpty()) {
                    builder.h4("${response.code.toString()} - ${definition.title.ifEmpty { "数据结构" }}")
                    buildDefinitionProperties(builder, definition.properties())
                }
            }
        }
    }
    
    /**
     * 构建定义属性表格
     */
    private fun buildDefinitionProperties(builder: WordDocumentBuilder, properties: List<DefinitionProperty>) {
        val tableBuilder = builder.table(properties.size + 1, 5)
        
        // 表头
        tableBuilder.hCell("字段名称")
            .hCell("字段类型")
            .hCell("是否必需")
            .hCell("字段描述")
            .hCell("示例值")
        
        // 属性行
        properties.forEach { prop: DefinitionProperty ->
            tableBuilder.newRow()
                .cell(prop.name)
                .cell(getPropertyType(prop))
                .cell(if (prop.required) "是" else "否")
                .cell(prop.description)
                .cell(getPropertyExample(prop))
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