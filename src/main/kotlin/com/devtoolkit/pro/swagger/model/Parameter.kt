package com.devtoolkit.pro.swagger.model

/**
 * Swagger参数信息
 */
data class Parameter(
    var name: String = "",
    var `in`: String = "",
    var description: String = "",
    var required: Boolean = false,
    var type: String = "",
    var collectionFormat: String = "",
    var enums: List<String> = emptyList(),
    var originalRef: String = "",
    var ref: Definition? = null,
    var items: Parameter? = null,
    var refDefinition: Definition? = null
) {
    fun name(name: String) = apply { this.name = name }
    fun `in`(`in`: String) = apply { this.`in` = `in` }
    fun description(description: String) = apply { this.description = description }
    fun required(required: Boolean) = apply { this.required = required }
    fun type(type: String) = apply { this.type = type }
    fun collectionFormat(collectionFormat: String) = apply { this.collectionFormat = collectionFormat }
    fun enums(enums: List<String>) = apply { this.enums = enums }
    fun originalRef(originalRef: String) = apply { this.originalRef = originalRef }
    fun ref(ref: Definition?) = apply { this.ref = ref }
    fun items(items: Parameter?) = apply { this.items = items }
    fun refDefinition(refDefinition: Definition?) = apply { this.refDefinition = refDefinition }
}