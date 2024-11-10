package me.centralhardware.znatoki.telegram.statistic.report

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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExcelDsl(
    private val fio: String,
    private val serviceName: String,
    private val date: LocalDateTime,
) {
    private val workbook: Workbook = XSSFWorkbook()

    fun sheet(name: String, initializer: SheetDsl.() -> Unit) {
        SheetDsl(workbook.createSheet(name)).apply(initializer)
    }

    fun build(): File {
        return try {
            val temp =
                Files.createFile(
                        Path.of(
                            "$fio - $serviceName ${date.format(DateTimeFormatter.ofPattern("MMMM"))} ${date.year}.xlsx"
                        )
                    )
                    .toFile()
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
        RowDsl(sheet, rowIndex).apply(initializer).build()
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
    private val cells: MutableList<Pair<String, HorizontalAlignment>> = mutableListOf()

    fun emptyCell() = cell("")

    fun <T> cell(t: T, align: HorizontalAlignment = HorizontalAlignment.CENTER) =
        cells.add(Pair(t.toString(), align))

    fun build() {
        val row = sheet.createRow(rowIndex)
        cells.forEachIndexed { i, v ->
            if (!styleCache.containsKey(v.second)) {
                styleCache[v.second] = sheet.workbook.createCellStyle().apply {
                    alignment = v.second
                }
            }

            val cell = row.createCell(i)
            cell.setCellValue(v.first)
            cell.cellStyle = styleCache[v.second]
            sheet.autoSizeColumn(i)
        }
    }
}

fun excel(
    fio: String,
    serviceName: String,
    date: LocalDateTime,
    initializer: ExcelDsl.() -> Unit,
): ExcelDsl {
    return ExcelDsl(fio, serviceName, date).apply(initializer)
}
