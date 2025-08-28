package com.devtoolkit.pro.database.model

/**
 * 数据库表信息
 */
data class TableInfo(
    val name: String,
    val comment: String?,
    val engine: String?,
    val collation: String?,
    val columns: List<ColumnInfo>
)

/**
 * 数据库列信息
 */
data class ColumnInfo(
    val name: String,
    val type: String,
    val comment: String?,
    val nullable: Boolean,
    val defaultValue: String?,
    val isPrimaryKey: Boolean,
    val collation: String?
)

/**
 * 表分组配置
 */
data class TableGroupConfig(
    val groupName: String,
    val tables: MutableList<String> = mutableListOf()
) {
    fun addTable(tableName: String): TableGroupConfig {
        tables.add(tableName)
        return this
    }
}

/**
 * 数据库文档生成配置
 */
data class DatabaseDocConfig(
    val title: String,
    val outputPath: String
)