package com.devtoolkit.pro.swagger

import com.devtoolkit.pro.swagger.model.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.util.io.HttpRequests
import java.util.*

/**
 * Swagger解析器
 */
class SwaggerParser {
    private val objectMapper = ObjectMapper()
    
    /**
     * 解析swagger URL，返回按标签分组的API列表
     */
    fun parse(url: String): Map<String, List<SwaggerApi>> {
        val groups = mutableMapOf<String, MutableList<SwaggerApi>>()
        val node = request(url)
        
        // 初始化分组
        node.get("tags")?.forEach { tagNode ->
            val tagName = tagNode.get("name")?.asText() ?: ""
            groups[tagName] = mutableListOf()
        }
        
        // 解析definitions
        val definitions = parseDefinitions(node.get("definitions"))
        
        // 解析APIs
        val apis = parseApis(node.get("paths"), definitions)
        
        // 按标签分组
        apis.forEach { api ->
            api.tags.forEach { tag ->
                groups[tag]?.add(api) ?: run {
                    println("Warning: Unknown tag $tag for API: $api")
                }
            }
        }
        
        return groups.mapValues { it.value.toList() }
    }
    
    /**
     * 解析API路径
     */
    private fun parseApis(pathsNode: JsonNode?, definitions: Map<String, Definition>): List<SwaggerApi> {
        val apis = mutableListOf<SwaggerApi>()
        pathsNode?.fields()?.forEach { (path, pathNode) ->
            pathNode.fields().forEach { (method, methodNode) ->
                val api = SwaggerApi()
                    .path(path)
                    .method(method)
                    .summary(methodNode.get("summary")?.asText() ?: "")
                    .description(methodNode.get("description")?.asText() ?: "")
                    .operationId(methodNode.get("operationId")?.asText() ?: "")
                    .deprecated(methodNode.get("deprecated")?.asBoolean() ?: false)
                    .tags(parseStringArray(methodNode.get("tags")))
                    .consumes(parseStringArray(methodNode.get("consumes")))
                    .produces(parseStringArray(methodNode.get("produces")))
                    .responses(parseResponses(methodNode.get("responses"), definitions))
                    .parameters(parseParameters(methodNode.get("parameters"), definitions))
                apis.add(api)
            }
        }
        return apis
    }
    
    /**
     * 解析参数
     */
    private fun parseParameters(parametersNode: JsonNode?, definitions: Map<String, Definition>): List<Parameter> {
        val parameters = mutableListOf<Parameter>()
        parametersNode?.forEach { paramNode ->
            val parameter = Parameter()
                .name(paramNode.get("name")?.asText() ?: "")
                .`in`(paramNode.get("in")?.asText() ?: "")
                .description(paramNode.get("description")?.asText() ?: "")
                .required(paramNode.get("required")?.asBoolean() ?: false)
                .type(paramNode.get("type")?.asText() ?: "")
                .collectionFormat(paramNode.get("collectionFormat")?.asText() ?: "")
                .enums(parseStringArray(paramNode.get("enum")))
            
            // 处理schema引用
            paramNode.get("schema")?.let { schemaNode ->
                schemaNode.get("originalRef")?.asText()?.let { originalRef ->
                    parameter.originalRef(originalRef)
                    schemaNode.get("\$ref")?.asText()?.let { ref ->
                        parameter.ref(definitions[ref])
                    }
                }
            }
            
            // 处理items引用
            paramNode.get("items")?.get("\$ref")?.asText()?.let { ref ->
                parameter.refDefinition(definitions[ref])
            }
            
            parameters.add(parameter)
        }
        return parameters
    }
    
    /**
     * 解析响应
     */
    private fun parseResponses(responsesNode: JsonNode?, definitions: Map<String, Definition>): List<Response> {
        val responses = mutableListOf<Response>()
        responsesNode?.fields()?.forEach { (code, responseNode) ->
            val response = Response()
                .code(code.toIntOrNull() ?: 0)
                .description(responseNode.get("description")?.asText() ?: "")
            
            responseNode.get("schema")?.get("\$ref")?.asText()?.let { ref ->
                response.refDefinition(definitions[ref])
            }
            
            responses.add(response)
        }
        return responses
    }
    
    /**
     * 解析定义
     */
    private fun parseDefinitions(definitionsNode: JsonNode?): Map<String, Definition> {
        val definitions = mutableMapOf<String, Definition>()
        val queue: Queue<Definition> = LinkedList()
        
        definitionsNode?.fields()?.forEach { (key, defNode) ->
            val definition = Definition()
                .title(defNode.get("title")?.asText() ?: "")
                .type(defNode.get("type")?.asText() ?: "")
                .required(parseStringArray(defNode.get("required")))
            
            defNode.get("properties")?.fields()?.forEach { (propKey, propNode) ->
                val property = DefinitionProperty()
                    .name(propKey)
                    .type(propNode.get("type")?.asText() ?: "")
                    .format(propNode.get("format")?.asText() ?: "")
                    .description(propNode.get("description")?.asText() ?: "")
                    .originalRef(propNode.get("originalRef")?.asText() ?: "")
                    .required(definition.required.contains(propKey))
                
                // 处理items
                propNode.get("items")?.let { itemsNode ->
                    val item = DefinitionProperty()
                        .type(itemsNode.get("type")?.asText() ?: "")
                        .format(itemsNode.get("format")?.asText() ?: "")
                        .description(propNode.get("description")?.asText() ?: "")
                        .originalRef(itemsNode.get("originalRef")?.asText() ?: "")
                    property.items(item)
                }
                
                definition.addProperty(property)
                queue.add(definition)
            }
            
            definitions["#/definitions/$key"] = definition
        }
        
        // 设置引用关系
        while (queue.isNotEmpty()) {
            val def = queue.poll()
            def.properties().forEach { property ->
                if (property.originalRef.isNotEmpty()) {
                    property.refDefinition(definitions["#/definitions/${property.originalRef}"])
                }
                property.items?.let { item ->
                    if (item.originalRef.isNotEmpty()) {
                        item.refDefinition(definitions["#/definitions/${item.originalRef}"])
                    }
                }
            }
        }
        
        return definitions
    }
    
    /**
     * 解析字符串数组
     */
    private fun parseStringArray(node: JsonNode?): List<String> {
        val result = mutableListOf<String>()
        node?.forEach { item ->
            result.add(item.asText())
        }
        return result
    }
    
    /**
     * 请求swagger数据
     */
    private fun request(url: String): JsonNode {
        return try {
            val content = HttpRequests.request(url)
                .readString()
            objectMapper.readTree(content)
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch swagger data from $url", e)
        }
    }
}