package com.devtoolkit.pro.services

import com.github.vertical_blank.sqlformatter.SqlFormatter
import com.github.vertical_blank.sqlformatter.languages.Dialect

/**
 * SQL格式化服务
 * 提供SQL格式化和压缩功能
 */
class SqlFormatterService {
    
    /**
     * 格式化SQL语句
     * @param sql 原始SQL语句
     * @return 格式化后的SQL语句
     */
    fun formatSql(sql: String): String {
        if (sql.isBlank()) {
            return sql
        }
        
        return try {
            // 使用sql-formatter库格式化SQL
            SqlFormatter.of(Dialect.StandardSql)
                .format(sql.trim())
        } catch (e: Exception) {
            // 如果格式化失败，返回原始SQL
            sql.trim()
        }
    }
    
    /**
     * 压缩SQL语句为一行
     * @param sql 原始SQL语句
     * @return 压缩后的SQL语句
     */
    fun compressSql(sql: String): String {
        if (sql.isBlank()) {
            return sql
        }
        
        return sql.trim()
            .replace("\r\n", " ")  // 替换Windows换行符
            .replace("\n", " ")    // 替换Unix换行符
            .replace("\r", " ")    // 替换Mac换行符
            .replace("\t", " ")    // 替换制表符
            .replace(Regex("\\s+"), " ") // 将多个空格替换为单个空格
            .trim()
    }
    
    /**
     * 检查SQL语句是否有效
     * @param sql SQL语句
     * @return 是否有效
     */
    fun isValidSql(sql: String): Boolean {
        if (sql.isBlank()) {
            return false
        }
        
        val trimmedSql = sql.trim().uppercase()
        
        // 简单的SQL关键字检查
        val sqlKeywords = listOf(
            "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", 
            "ALTER", "TRUNCATE", "WITH", "SHOW", "DESCRIBE", "EXPLAIN"
        )
        
        return sqlKeywords.any { keyword -> 
            trimmedSql.startsWith(keyword)
        }
    }
}