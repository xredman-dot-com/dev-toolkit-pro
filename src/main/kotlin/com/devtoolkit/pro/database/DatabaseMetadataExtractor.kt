package com.devtoolkit.pro.database

import com.devtoolkit.pro.database.model.TableInfo
import com.devtoolkit.pro.database.model.ColumnInfo
import com.intellij.database.model.DasDataSource
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasColumn
import com.intellij.database.util.DasUtil

/**
 * 数据库元数据提取器
 */
class DatabaseMetadataExtractor {
    
    /**
     * 从数据源提取所有表信息
     */
    fun extractTablesFromDataSource(dataSource: DasDataSource): List<TableInfo> {
        val tables = mutableListOf<TableInfo>()
        
        try {
            // 获取数据源中的所有表
            val dasTables = DasUtil.getTables(dataSource)
            
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
     * 提取单个表的信息
     */
    private fun extractTableInfo(dasTable: DasTable): TableInfo {
        val columns = mutableListOf<ColumnInfo>()
        
        // 提取列信息
        val dasColumns = DasUtil.getColumns(dasTable)
        for (dasColumn in dasColumns) {
            val columnInfo = extractColumnInfo(dasColumn)
            columns.add(columnInfo)
        }
        
        return TableInfo(
            name = dasTable.name,
            comment = dasTable.comment,
            engine = null, // DAS API可能不直接提供引擎信息
            collation = null, // DAS API可能不直接提供排序规则信息
            columns = columns
        )
    }
    
    /**
     * 提取单个列的信息
     */
    private fun extractColumnInfo(dasColumn: DasColumn): ColumnInfo {
        return ColumnInfo(
            name = dasColumn.name,
            type = dasColumn.dasType?.specification ?: "UNKNOWN",
            comment = dasColumn.comment,
            nullable = !dasColumn.isNotNull,
            defaultValue = dasColumn.default,
            isPrimaryKey = DasUtil.isPrimary(dasColumn),
            collation = null // DAS API可能不直接提供排序规则信息
        )
    }
}