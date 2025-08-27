package com.devtoolkit.pro.swagger.model

/**
 * Swagger定义信息
 */
data class Definition(
    var title: String = "",
    var type: String = "",
    var required: List<String> = emptyList(),
    private val _properties: MutableList<DefinitionProperty> = mutableListOf()
) {
    fun title(title: String) = apply { this.title = title }
    fun type(type: String) = apply { this.type = type }
    fun required(required: List<String>) = apply { this.required = required }
    
    fun addProperty(property: DefinitionProperty) = apply { _properties.add(property) }
    fun properties(): List<DefinitionProperty> = _properties.toList()
}