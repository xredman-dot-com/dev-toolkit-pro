package com.devtoolkit.pro.database

import com.devtoolkit.pro.database.model.TableInfo
import com.devtoolkit.pro.database.model.ColumnInfo

/**
 * 数据库元数据提取器
 */
class DatabaseMetadataExtractor {
    
    /**
     * 从数据源提取所有表信息
     */
    fun extractTablesFromDataSource(dataSource: Any): List<TableInfo> {
        val tables = mutableListOf<TableInfo>()
        
        try {
            // 使用反射获取数据源中的所有表
            val dasTables = getTablesSafely(dataSource)
            
            for (dasTable in dasTables) {
                val tableInfo = extractTableInfo(dasTable)
                tables.add(tableInfo)
            }
        } catch (e: Exception) {
            println("Error extracting tables from data source: ${e.message}")
            e.printStackTrace()
        }
        
        return tables
    }
    
    /**
     * 使用反射安全地获取表列表
     */
    private fun getTablesSafely(dataSource: Any): List<Any> {
        return try {
            val dasUtilClass = Class.forName("com.intellij.database.util.DasUtil")
            val getTablesMethod = dasUtilClass.getMethod("getTables", Class.forName("com.intellij.database.model.DasDataSource"))
            val tables = getTablesMethod.invoke(null, dataSource) as Collection<*>
            tables.filterNotNull()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 提取单个表的信息
     */
    private fun extractTableInfo(dasTable: Any): TableInfo {
        val columns = mutableListOf<ColumnInfo>()
        
        try {
            // 使用反射提取列信息
            val dasColumns = getColumnsSafely(dasTable)
            for (dasColumn in dasColumns) {
                val columnInfo = extractColumnInfo(dasColumn)
                columns.add(columnInfo)
            }
            
            return TableInfo(
                name = getTableName(dasTable),
                comment = getTableComment(dasTable),
                engine = null, // DAS API可能不直接提供引擎信息
                collation = null, // DAS API可能不直接提供排序规则信息
                columns = columns
            )
        } catch (e: Exception) {
            return TableInfo(
                name = "Unknown",
                comment = null,
                engine = null,
                collation = null,
                columns = emptyList()
            )
        }
    }
    
    /**
     * 使用反射安全地获取列列表
     */
    private fun getColumnsSafely(dasTable: Any): List<Any> {
        return try {
            val dasUtilClass = Class.forName("com.intellij.database.util.DasUtil")
            val getColumnsMethod = dasUtilClass.getMethod("getColumns", Class.forName("com.intellij.database.model.DasTable"))
            val columns = getColumnsMethod.invoke(null, dasTable) as Collection<*>
            columns.filterNotNull()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 使用反射获取表名
     */
    private fun getTableName(dasTable: Any): String {
        return try {
            val nameMethod = dasTable.javaClass.getMethod("getName")
            nameMethod.invoke(dasTable) as? String ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 使用反射获取表注释
     */
    private fun getTableComment(dasTable: Any): String? {
        return try {
            val commentMethod = dasTable.javaClass.getMethod("getComment")
            commentMethod.invoke(dasTable) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 提取单个列的信息
     */
    private fun extractColumnInfo(dasColumn: Any): ColumnInfo {
        return try {
            ColumnInfo(
                name = getColumnName(dasColumn),
                type = getColumnType(dasColumn),
                comment = getColumnComment(dasColumn),
                nullable = !getColumnNotNull(dasColumn),
                defaultValue = getColumnDefault(dasColumn),
                isPrimaryKey = getColumnIsPrimary(dasColumn),
                collation = null // DAS API可能不直接提供排序规则信息
            )
        } catch (e: Exception) {
            ColumnInfo(
                name = "Unknown",
                type = "UNKNOWN",
                comment = null,
                nullable = true,
                defaultValue = null,
                isPrimaryKey = false,
                collation = null
            )
        }
    }
    
    /**
     * 使用反射获取列名
     */
    private fun getColumnName(dasColumn: Any): String {
        return try {
            val nameMethod = dasColumn.javaClass.getMethod("getName")
            nameMethod.invoke(dasColumn) as? String ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 使用反射获取列类型
     */
    private fun getColumnType(dasColumn: Any): String {
        return try {
            val dasTypeMethod = dasColumn.javaClass.getMethod("getDasType")
            val dasType = dasTypeMethod.invoke(dasColumn)
            if (dasType != null) {
                val specificationMethod = dasType.javaClass.getMethod("getSpecification")
                specificationMethod.invoke(dasType) as? String ?: "UNKNOWN"
            } else {
                "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
    
    /**
     * 使用反射获取列注释
     */
    private fun getColumnComment(dasColumn: Any): String? {
        return try {
            val commentMethod = dasColumn.javaClass.getMethod("getComment")
            commentMethod.invoke(dasColumn) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 使用反射获取列是否非空
     */
    private fun getColumnNotNull(dasColumn: Any): Boolean {
        return try {
            val isNotNullMethod = dasColumn.javaClass.getMethod("isNotNull")
            isNotNullMethod.invoke(dasColumn) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 使用反射获取列默认值
     */
    private fun getColumnDefault(dasColumn: Any): String? {
        return try {
            val defaultMethod = dasColumn.javaClass.getMethod("getDefault")
            defaultMethod.invoke(dasColumn) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 使用反射获取列是否为主键
     */
    private fun getColumnIsPrimary(dasColumn: Any): Boolean {
        return try {
            val dasUtilClass = Class.forName("com.intellij.database.util.DasUtil")
            val isPrimaryMethod = dasUtilClass.getMethod("isPrimary", Class.forName("com.intellij.database.model.DasColumn"))
            isPrimaryMethod.invoke(null, dasColumn) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}