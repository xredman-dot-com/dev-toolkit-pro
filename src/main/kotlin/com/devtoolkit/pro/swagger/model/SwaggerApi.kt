package com.devtoolkit.pro.swagger.model

/**
 * Swagger API信息
 */
data class SwaggerApi(
    var path: String = "",
    var method: String = "",
    var tags: List<String> = emptyList(),
    var summary: String = "",
    var description: String = "",
    var operationId: String = "",
    var consumes: List<String> = emptyList(),
    var produces: List<String> = emptyList(),
    var deprecated: Boolean = false,
    var parameters: List<Parameter> = emptyList(),
    var responses: List<Response> = emptyList()
) {
    fun path(path: String) = apply { this.path = path }
    fun method(method: String) = apply { this.method = method }
    fun summary(summary: String) = apply { this.summary = summary }
    fun description(description: String) = apply { this.description = description }
    fun operationId(operationId: String) = apply { this.operationId = operationId }
    fun deprecated(deprecated: Boolean) = apply { this.deprecated = deprecated }
    fun tags(tags: List<String>) = apply { this.tags = tags }
    fun consumes(consumes: List<String>) = apply { this.consumes = consumes }
    fun produces(produces: List<String>) = apply { this.produces = produces }
    fun parameters(parameters: List<Parameter>) = apply { this.parameters = parameters }
    fun responses(responses: List<Response>) = apply { this.responses = responses }
}