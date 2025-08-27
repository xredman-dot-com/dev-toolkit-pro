package com.devtoolkit.pro.document

import org.apache.poi.wp.usermodel.HeaderFooterType
import org.apache.poi.xwpf.usermodel.*
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger

/**
 * Word文档构建器
 */
class WordDocumentBuilder {
    companion object {
        // 公文标准字体
        const val FONT_EAST_ASIA = "仿宋_GB2312"
        const val FONT_ASCII = "Times New Roman"
        const val FONT_HANSI = "Times New Roman"
        const val FONT_SONTI = "仿宋_GB2312"
        const val FONT_COURIER_NEW = "Courier New"
        
        // 公文标准字号
        const val FONT_SIZE_TITLE = 22  // 二号
        const val FONT_SIZE_H1 = 16     // 三号
        const val FONT_SIZE_H2 = 14     // 四号
        const val FONT_SIZE_NORMAL = 12 // 小四号
        const val FONT_SIZE_SMALL = 10  // 五号
        
        fun newBuilder(): WordDocumentBuilder = WordDocumentBuilder()
    }
    
    private val doc = XWPFDocument()
    
    init {
        initializeStyles()
        setupPageMargins()
    }
    
    /**
     * 初始化样式
     */
    private fun initializeStyles() {
        createStyle("标题 1", 1, FONT_SIZE_H1)
        createStyle("标题 2", 2, FONT_SIZE_H2)
        createStyle("标题 3", 3, FONT_SIZE_NORMAL)
        createStyle("标题 4", 4, FONT_SIZE_NORMAL)
        createStyle("标题 5", 5, FONT_SIZE_SMALL)
        createStyle("标题 6", 6, FONT_SIZE_SMALL)
        createStyle("标题 7", 7, FONT_SIZE_SMALL)
    }
    
    /**
     * 设置页面边距
     */
    private fun setupPageMargins() {
        val ctSectPr = if (doc.document.body.isSetSectPr) {
            doc.document.body.sectPr
        } else {
            doc.document.body.addNewSectPr()
        }
        val ctPageMar = if (ctSectPr.isSetPgMar) ctSectPr.pgMar else ctSectPr.addNewPgMar()
        ctPageMar.left = BigInteger.valueOf(1587L)
        ctPageMar.top = BigInteger.valueOf(2097L)
        ctPageMar.right = BigInteger.valueOf(1587L)
        ctPageMar.bottom = BigInteger.valueOf(2097L)
    }
    
    /**
     * 设置页眉
     */
    fun header(headerText: String): WordDocumentBuilder {
        val headers = doc.headerList
        if (headers.isEmpty()) {
            val p = doc.createHeader(HeaderFooterType.DEFAULT).createParagraph()
            val run = p.createRun()
            p.alignment = ParagraphAlignment.CENTER
            run.setText(headerText)
            run.fontFamily = "阿里巴巴普惠体"
            run.fontSize = 7
            p.spacingBetween = 1.5
            p.borderBottom = Borders.THICK
        }
        return this
    }
    
    /**
     * 设置标题
     */
    fun title(title: String, subTitle: String): WordDocumentBuilder {
        doc.createParagraph().spacingBetween = 2.0
        doc.createParagraph().spacingBetween = 2.0
        
        // 主标题
        val titleP = doc.createParagraph()
        titleP.alignment = ParagraphAlignment.CENTER
        val titleRun = titleP.createRun()
        titleRun.setText(title)
        titleRun.fontFamily = FONT_EAST_ASIA
        titleRun.fontSize = FONT_SIZE_TITLE
        titleP.spacingBetween = 1.5
        titleP.borderBottom = Borders.THIN_THICK_LARGE_GAP
        
        // 副标题
        val subTitleP = doc.createParagraph()
        subTitleP.alignment = ParagraphAlignment.CENTER
        val subTitleRun = subTitleP.createRun()
        subTitleRun.setText(subTitle)
        subTitleRun.fontFamily = FONT_EAST_ASIA
        subTitleRun.fontSize = FONT_SIZE_TITLE
        subTitleP.spacingBetween = 2.0
        subTitleP.borderTop = Borders.THICK_THIN_SMALL_GAP
        
        subTitleRun.addBreak(BreakType.PAGE)
        return this
    }
    
    /**
     * 添加一级标题
     */
    fun h1(text: String): WordDocumentBuilder = h("标题 1", text)
    
    /**
     * 添加二级标题
     */
    fun h2(text: String): WordDocumentBuilder = h("标题 2", text)
    
    /**
     * 添加三级标题
     */
    fun h3(text: String): WordDocumentBuilder = h("标题 3", text)
    
    /**
     * 添加四级标题
     */
    fun h4(text: String): WordDocumentBuilder = h("标题 4", text)
    
    /**
     * 添加五级标题
     */
    fun h5(text: String): WordDocumentBuilder = h("标题 5", text)
    
    /**
     * 添加六级标题
     */
    fun h6(text: String): WordDocumentBuilder = h("标题 6", text)
    
    /**
     * 添加七级标题
     */
    fun h7(text: String): WordDocumentBuilder = h("标题 7", text)
    
    /**
     * 添加标题
     */
    private fun h(styleId: String, text: String): WordDocumentBuilder {
        val p = doc.createParagraph()
        p.style = styleId
        p.createRun().setText(text)
        return this
    }
    
    /**
     * 创建表格
     */
    fun table(rows: Int, cols: Int): WordTableBuilder {
        return WordTableBuilder(this, rows, cols)
    }
    
    /**
     * 构建文档
     */
    fun build(file: File) {
        FileOutputStream(file).use { fos ->
            doc.write(fos)
            doc.close()
        }
    }
    
    /**
     * 创建样式
     */
    private fun createStyle(styleId: String, level: Int, fontSize: Int) {
        val ctStyle = CTStyle.Factory.newInstance()
        ctStyle.styleId = styleId
        
        // 设置样式名称
        val styleName = CTString.Factory.newInstance()
        styleName.`val` = styleId
        ctStyle.name = styleName
        
        // 设置段落标题等级
        val ctDecimalNumber = CTDecimalNumber.Factory.newInstance()
        ctDecimalNumber.`val` = BigInteger.valueOf(level.toLong())
        ctStyle.uiPriority = ctDecimalNumber
        
        // 设置字体大小
        val rpr = if (ctStyle.isSetRPr) ctStyle.rPr else ctStyle.addNewRPr()
        val szcs = if (rpr.isSetSzCs) rpr.szCs else rpr.addNewSzCs()
        val sz = if (rpr.isSetSz) rpr.sz else rpr.addNewSz()
        szcs.`val` = BigInteger.valueOf(fontSize.toLong())
        sz.`val` = BigInteger.valueOf(fontSize.toLong())
        
        // 设置字体
        val fonts = if (rpr.isSetRFonts) rpr.rFonts else rpr.addNewRFonts()
        fonts.eastAsia = FONT_EAST_ASIA
        fonts.ascii = FONT_ASCII
        fonts.hAnsi = FONT_HANSI
        
        val ctOnOff = CTOnOff.Factory.newInstance()
        ctStyle.unhideWhenUsed = ctOnOff
        ctStyle.qFormat = ctOnOff
        
        val ppr = CTPPr.Factory.newInstance()
        ppr.outlineLvl = ctDecimalNumber
        
        // 设置段落前后距离
        val ctSpacing = if (ppr.isSetSpacing) ppr.spacing else ppr.addNewSpacing()
        ctSpacing.before = BigInteger.valueOf(12 * 20L)
        ctSpacing.after = BigInteger.valueOf(8 * 20L)
        ctStyle.pPr = ppr
        
        val ctNumPr = if (ppr.isSetNumPr) ppr.numPr else ppr.addNewNumPr()
        
        // 增加XWPF样式
        val style = XWPFStyle(ctStyle)
        style.type = STStyleType.PARAGRAPH
        val styles = doc.createStyles()
        styles.addStyle(style)
    }
    
    /**
     * 表格构建器
     */
    inner class WordTableBuilder(private val wpBuilder: WordDocumentBuilder, rows: Int, cols: Int) {
        private val table: XWPFTable = wpBuilder.doc.createTable(rows, cols)
        private var row = 0
        private var col = 0
        
        init {
            table.setCellMargins(0, 10, 0, 50)
            // 设置表格宽度为100%
            setTableWidth()
        }
        
        /**
         * 设置表格宽度为100%
         */
        private fun setTableWidth() {
            val ctTbl = table.ctTbl
            val ctTblPr = if (ctTbl.tblPr != null) ctTbl.tblPr else ctTbl.addNewTblPr()
            val ctTblWidth = if (ctTblPr.tblW != null) ctTblPr.tblW else ctTblPr.addNewTblW()
            ctTblWidth.type = STTblWidth.PCT
            ctTblWidth.w = BigInteger.valueOf(5000) // 100% = 5000
        }
        
        /**
         * 设置列宽度
         */
        fun setColumnWidths(vararg widths: Int): WordTableBuilder {
            val ctTbl = table.ctTbl
            val ctTblGrid = if (ctTbl.tblGrid != null) ctTbl.tblGrid else ctTbl.addNewTblGrid()
            
            // 清除现有的列定义
            ctTblGrid.gridColList.clear()
            
            // 添加新的列宽度定义
            widths.forEach { width ->
                val ctGridCol = ctTblGrid.addNewGridCol()
                ctGridCol.w = BigInteger.valueOf(width.toLong())
            }
            
            return this
        }
        
        /**
         * 新行
         */
        fun newRow(): WordTableBuilder {
            row++
            col = 0
            if (row == table.rows.size) {
                table.createRow()
            }
            return this
        }
        
        /**
         * 键值对单元格
         */
        fun kv(title: String, content: String): WordTableBuilder {
            val tRow = table.getRow(row)
            var tCell = tRow.getCell(col++)
            setCell(tCell, title, true)
            tCell = tRow.getCell(col++)
            setCell(tCell, content, false)
            return this
        }
        
        /**
         * 标题单元格
         */
        fun hCell(text: String): WordTableBuilder {
            val tr = table.getRow(row)
            val tc = tr.getCell(col++)
            setCell(tc, text, true)
            return this
        }
        
        /**
         * 普通单元格
         */
        fun cell(text: String): WordTableBuilder {
            val tr = table.getRow(row)
            val tc = tr.getCell(col++)
            setCell(tc, text, false)
            return this
        }
        
        /**
         * 空白单元格
         */
        fun blank(): WordTableBuilder {
            col++
            return this
        }
        
        /**
         * 水平合并单元格
         */
        fun mergeHCell(begin: Int, end: Int): WordTableBuilder {
            val tRow = table.getRow(row)
            val tCell = tRow.getCell(begin)
            val ctTc = tCell.ctTc
            val ctTcPr = if (ctTc.isSetTcPr) ctTc.tcPr else ctTc.addNewTcPr()
            
            val cthMerge = if (ctTcPr.isSetHMerge) ctTcPr.hMerge else ctTcPr.addNewHMerge()
            cthMerge.`val` = STMerge.RESTART
            
            // 进行单元格合并
            for (k in begin + 1..end) {
                val cell = tRow.getCell(k)
                val tc = cell.ctTc
                val tcPr = if (tc.isSetTcPr) tc.tcPr else tc.addNewTcPr()
                val hMerge = if (tcPr.isSetHMerge) tcPr.hMerge else tcPr.addNewHMerge()
                hMerge.`val` = STMerge.CONTINUE
            }
            return this
        }
        
        /**
         * 添加合并单元格（跨列）
         */
        fun mergedCell(text: String, colspan: Int, isHeader: Boolean = false): WordTableBuilder {
            val tr = table.getRow(row)
            val tc = tr.getCell(col)
            setCell(tc, text, isHeader)
            
            if (colspan > 1) {
                mergeHCell(col, col + colspan - 1)
            }
            col += colspan
            return this
        }
        
        /**
         * 结束表格构建
         */
        fun end(): WordDocumentBuilder = wpBuilder
        
        /**
         * 设置单元格内容
         */
        private fun setCell(cell: XWPFTableCell, text: String, isHeader: Boolean) {
            cell.verticalAlignment = XWPFTableCell.XWPFVertAlign.TOP
            
            val ctTc = cell.ctTc
            val ctTcPr = if (ctTc.isSetTcPr) ctTc.tcPr else ctTc.addNewTcPr()
            val ctShd = if (ctTcPr.isSetShd) ctTcPr.shd else ctTcPr.addNewShd()
            
            // 设置背景颜色
            if (isHeader) {
                ctShd.fill = "f2f2f2"
            }
            
            // 设置单元格边距
            val ctTcMar = if (ctTcPr.isSetTcMar) ctTcPr.tcMar else ctTcPr.addNewTcMar()
            val ctTblWidth = CTTblWidth.Factory.newInstance()
            ctTblWidth.w = BigInteger.valueOf(100)
            ctTblWidth.type = STTblWidth.DXA
            ctTcMar.left = ctTblWidth
            ctTcMar.right = ctTblWidth
            ctTcMar.top = CTTblWidth.Factory.newInstance().apply {
                w = BigInteger.valueOf(50)
                type = STTblWidth.DXA
            }
            ctTcMar.bottom = CTTblWidth.Factory.newInstance().apply {
                w = BigInteger.valueOf(50)
                type = STTblWidth.DXA
            }
            
            // 创建内部内容
            val p = if (cell.paragraphs.isNotEmpty()) cell.paragraphs[0] else cell.addParagraph()
            p.alignment = ParagraphAlignment.LEFT
            
            // 设置段落间距
            val ppr = if (p.ctp.isSetPPr) p.ctp.pPr else p.ctp.addNewPPr()
            val ctSpacing = if (ppr.isSetSpacing) ppr.spacing else ppr.addNewSpacing()
            ctSpacing.after = BigInteger.valueOf(0)
            ctSpacing.before = BigInteger.valueOf(0)
            ctSpacing.lineRule = STLineSpacingRule.AUTO
            ctSpacing.line = BigInteger.valueOf(240) // 1.2倍行距
            
            val run = p.createRun()
            if (text.isNotEmpty()) {
                // 处理长文本换行
                val lines = text.split("\n")
                lines.forEachIndexed { index, line ->
                    if (index > 0) {
                        run.addBreak()
                    }
                    run.setText(line)
                }
            }
            
            run.fontSize = if (isHeader) FONT_SIZE_SMALL else FONT_SIZE_SMALL
            if (isHeader) {
                run.isBold = true
            }
            
            val ctrPr = if (run.ctr.isSetRPr) run.ctr.rPr else run.ctr.addNewRPr()
            val ctFonts = if (ctrPr.isSetRFonts) ctrPr.rFonts else ctrPr.addNewRFonts()
            if (isHeader) {
                ctFonts.eastAsia = FONT_EAST_ASIA
                ctFonts.ascii = FONT_ASCII
                ctFonts.hAnsi = FONT_HANSI
            } else {
                ctFonts.eastAsia = FONT_SONTI
                ctFonts.ascii = FONT_SONTI
                ctFonts.hAnsi = FONT_HANSI
            }
        }
    }
}