package com.devtoolkit.pro.swagger.model

/**
 * Swagger响应信息
 */
data class Response(
    var code: Int = 0,
    var description: String = "",
    var ref: Definition? = null,
    var refDefinition: Definition? = null
) {
    fun code(code: Int) = apply { this.code = code }
    fun description(description: String) = apply { this.description = description }
    fun ref(ref: Definition?) = apply { this.ref = ref }
    fun refDefinition(refDefinition: Definition?) = apply { this.refDefinition = refDefinition }
}