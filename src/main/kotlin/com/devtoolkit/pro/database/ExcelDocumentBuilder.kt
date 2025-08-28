package com.devtoolkit.pro.database

import com.devtoolkit.pro.database.model.TableGroupConfig
import com.devtoolkit.pro.database.model.TableInfo
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.awt.Color
import java.io.File
import java.io.FileOutputStream

/**
 * Excel数据库文档生成器
 */
class ExcelDocumentBuilder {
    
    private val workbook = XSSFWorkbook()
    private val titleStyle: CellStyle by lazy { createTitleStyle() }
    private val headerStyle: CellStyle by lazy { createHeaderStyle() }
    private val contentStyle: CellStyle by lazy { createContentStyle() }
    
    /**
     * 生成数据库文档
     */
    fun generateDocument(
        title: String,
        tables: List<TableInfo>,
        outputFile: File
    ) {
        // 按表名首字母分组
        val groupedTables = groupTablesByPrefix(tables)
        
        // 创建摘要页
        createSummarySheet(groupedTables)
        
        // 为每个分组创建详细页
        groupedTables.forEach { (groupName, tableList) ->
            createDetailSheet(groupName, tableList)
        }
        
        // 保存文件
        FileOutputStream(outputFile).use { outputStream ->
            workbook.write(outputStream)
        }
        
        workbook.close()
    }
    
    /**
     * 按表名前缀分组
     */
    private fun groupTablesByPrefix(tables: List<TableInfo>): Map<String, List<TableInfo>> {
        return tables.groupBy { table ->
            // 简单按表名首字母分组，可以根据需要调整分组逻辑
            val prefix = table.name.split("_").firstOrNull() ?: "其他"
            prefix.uppercase()
        }
    }
    
    /**
     * 创建摘要页
     */
    private fun createSummarySheet(groupedTables: Map<String, List<TableInfo>>) {
        val sheet = workbook.createSheet("摘要")
        
        // 设置列宽
        sheet.setColumnWidth(0, 5000)
        sheet.setColumnWidth(1, 10000)
        sheet.setColumnWidth(2, 14000)
        
        var rowIndex = 0
        
        // 创建表头
        val headerRow = sheet.createRow(rowIndex++)
        createCell(headerRow, 0, "分组", headerStyle)
        createCell(headerRow, 1, "表名称", headerStyle)
        createCell(headerRow, 2, "描述", headerStyle)
        
        // 填充数据
        groupedTables.forEach { (groupName, tableList) ->
            val startRow = rowIndex
            
            tableList.forEachIndexed { index, table ->
                val row = sheet.createRow(rowIndex++)
                
                if (index == 0) {
                    createCell(row, 0, groupName, contentStyle)
                }
                
                createCell(row, 1, table.name, contentStyle)
                createCell(row, 2, table.comment ?: "", contentStyle)
            }
            
            // 合并分组列
            if (tableList.size > 1) {
                sheet.addMergedRegion(CellRangeAddress(startRow, rowIndex - 1, 0, 0))
            }
        }
    }
    
    /**
     * 创建详细页
     */
    private fun createDetailSheet(groupName: String, tables: List<TableInfo>) {
        val sheet = workbook.createSheet(groupName)
        
        // 设置列宽
        sheet.setColumnWidth(0, 5000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(2, 6000)
        sheet.setColumnWidth(3, 4000)
        sheet.setColumnWidth(4, 8000)
        sheet.setColumnWidth(5, 15000)
        
        var rowIndex = 0
        
        tables.forEachIndexed { tableIndex, table ->
            // 表基本信息
            val tableInfoRow1 = sheet.createRow(rowIndex++)
            createCell(tableInfoRow1, 0, "表名称", titleStyle)
            createCell(tableInfoRow1, 1, table.name, contentStyle)
            createCell(tableInfoRow1, 3, "描述", titleStyle)
            createCell(tableInfoRow1, 4, table.comment ?: "", contentStyle)
            
            val tableInfoRow2 = sheet.createRow(rowIndex++)
            createCell(tableInfoRow2, 3, "字符集", titleStyle)
            createCell(tableInfoRow2, 4, table.collation ?: "", contentStyle)
            
            val tableInfoRow3 = sheet.createRow(rowIndex++)
            createCell(tableInfoRow3, 3, "存储引擎", titleStyle)
            createCell(tableInfoRow3, 4, table.engine ?: "", contentStyle)
            
            // 合并单元格
            sheet.addMergedRegion(CellRangeAddress(rowIndex - 3, rowIndex - 1, 0, 0))
            sheet.addMergedRegion(CellRangeAddress(rowIndex - 3, rowIndex - 1, 1, 2))
            sheet.addMergedRegion(CellRangeAddress(rowIndex - 2, rowIndex - 2, 4, 5))
            sheet.addMergedRegion(CellRangeAddress(rowIndex - 1, rowIndex - 1, 4, 5))
            
            // 列信息表头
            val columnHeaderRow = sheet.createRow(rowIndex++)
            createCell(columnHeaderRow, 0, "列名", headerStyle)
            createCell(columnHeaderRow, 1, "类型", headerStyle)
            createCell(columnHeaderRow, 2, "字符集", headerStyle)
            createCell(columnHeaderRow, 3, "是否为空", headerStyle)
            createCell(columnHeaderRow, 4, "缺省值", headerStyle)
            createCell(columnHeaderRow, 5, "描述", headerStyle)
            
            // 列信息数据
            table.columns.forEach { column ->
                val columnRow = sheet.createRow(rowIndex++)
                createCell(columnRow, 0, column.name, contentStyle)
                createCell(columnRow, 1, column.type, contentStyle)
                createCell(columnRow, 2, column.collation ?: "", contentStyle)
                createCell(columnRow, 3, if (column.nullable) "Yes" else "No", contentStyle)
                createCell(columnRow, 4, column.defaultValue ?: "", contentStyle)
                createCell(columnRow, 5, column.comment ?: "", contentStyle)
            }
            
            // 表之间添加空行
            if (tableIndex < tables.size - 1) {
                rowIndex++
            }
        }
    }
    
    /**
     * 创建单元格
     */
    private fun createCell(row: Row, columnIndex: Int, value: String, style: CellStyle): Cell {
        val cell = row.createCell(columnIndex)
        cell.setCellValue(value)
        cell.cellStyle = style
        return cell
    }
    
    /**
     * 创建标题样式
     */
    private fun createTitleStyle(): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.fontHeightInPoints = 12
        font.bold = true
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.LIGHT_TURQUOISE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        setBorders(style)
        return style
    }
    
    /**
     * 创建表头样式
     */
    private fun createHeaderStyle(): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.fontHeightInPoints = 11
        font.bold = true
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        setBorders(style)
        return style
    }
    
    /**
     * 创建内容样式
     */
    private fun createContentStyle(): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.fontHeightInPoints = 10
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.LEFT
        style.verticalAlignment = VerticalAlignment.CENTER
        setBorders(style)
        return style
    }
    
    /**
     * 设置边框
     */
    private fun setBorders(style: CellStyle) {
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
    }
}