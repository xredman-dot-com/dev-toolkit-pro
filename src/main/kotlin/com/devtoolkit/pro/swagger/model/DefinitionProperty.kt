package com.devtoolkit.pro.swagger.model

/**
 * Swagger定义属性信息
 */
data class DefinitionProperty(
    var name: String = "",
    var type: String = "",
    var format: String = "",
    var description: String = "",
    var originalRef: String = "",
    var required: Boolean = false,
    var refDefinition: Definition? = null,
    var items: DefinitionProperty? = null
) {
    fun name(name: String) = apply { this.name = name }
    fun type(type: String) = apply { this.type = type }
    fun format(format: String) = apply { this.format = format }
    fun description(description: String) = apply { this.description = description }
    fun originalRef(originalRef: String) = apply { this.originalRef = originalRef }
    fun required(required: Boolean) = apply { this.required = required }
    fun refDefinition(refDefinition: Definition?) = apply { this.refDefinition = refDefinition }
    fun items(items: DefinitionProperty?) = apply { this.items = items }
}