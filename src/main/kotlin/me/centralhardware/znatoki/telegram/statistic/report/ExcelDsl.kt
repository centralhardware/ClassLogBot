package me.centralhardware.znatoki.telegram.statistic.report

import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class ExcelDsl {
    private val workbook: Workbook = XSSFWorkbook()

    fun sheet(name: String, initializer: SheetDsl.() -> Unit) {
        SheetDsl(workbook.createSheet(name)).apply(initializer)
    }

    fun build(name: String): File {
        return try {
            val path = Path.of(name)
            Files.deleteIfExists(path)
            val temp = Files.createFile(path).toFile()
            val outputStream = FileOutputStream(temp)
            workbook.write(outputStream)
            workbook.close()
            temp
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}

class SheetDsl(private val sheet: Sheet) {
    private var rowIndex: Int = 0

    fun row(initializer: RowDsl.() -> Unit) {
        RowDsl(sheet, rowIndex).apply(initializer)
        rowIndex++
    }

    fun title(title: String, mergedCellCount: Int) {
        val row = sheet.createRow(rowIndex)
        sheet.autoSizeColumn(rowIndex)
        val cell = row.createCell(0)
        cell.setCellValue(title)
        val cellStyle = cell.cellStyle
        cellStyle.alignment = HorizontalAlignment.CENTER
        sheet.addMergedRegion(CellRangeAddress(rowIndex, rowIndex, 0, mergedCellCount))
        rowIndex++
    }
}

class RowDsl(private val sheet: Sheet, private val rowIndex: Int) {
    private val styleCache = mutableMapOf<HorizontalAlignment, CellStyle>()
    private val row = sheet.createRow(rowIndex)
    private val i = AtomicInteger(0)

    fun emptyCell() = cell("")

    private fun getStyle(align: HorizontalAlignment): CellStyle? {
        styleCache[align] = sheet.workbook.createCellStyle().apply {
            alignment = align
        }
        return styleCache[align]
    }

    fun <T> cell(t: T, align: HorizontalAlignment = HorizontalAlignment.CENTER) {
        val cell = row.createCell(i.get())
        cell.setCellValue(t.toString())
        cell.cellStyle = getStyle(align)
        sheet.autoSizeColumn(i.get())
        i.getAndIncrement()
    }

    fun cellHyperlink(url: String, caption: String) {
        val cell = row.createCell(i.get())
        cell.setCellValue(caption)
        val hyperlink = sheet.workbook.creationHelper.createHyperlink(HyperlinkType.URL)
        hyperlink.address = url
        cell.hyperlink = hyperlink
    }
}

fun excel(initializer: ExcelDsl.() -> Unit): ExcelDsl {
    return ExcelDsl().apply(initializer)
}
