package com.mydailylife.schedule.data

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import java.io.ByteArrayInputStream

/**
 * First-sheet reader for legacy Excel `.xls` (BIFF8 via Apache POI HSSF).
 */
internal object CourseXlsReader {
    private val formatter = DataFormatter()

    fun readSheetRows(bytes: ByteArray): List<List<String>> {
        if (bytes.size < 8) error("xls 文件过小或已损坏")
        // OLE compound document magic
        val oleMagic = byteArrayOf(
            0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
            0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte(),
        )
        if (!bytes.take(8).toByteArray().contentEquals(oleMagic)) {
            error("不是有效的 .xls 工作簿")
        }
        return try {
            HSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                    ?: error("xls 中没有工作表")
                val rows = mutableListOf<List<String>>()
                val lastRow = sheet.lastRowNum
                for (r in 0..lastRow) {
                    val row = sheet.getRow(r) ?: continue
                    val lastCell = row.lastCellNum.toInt().coerceAtLeast(0)
                    if (lastCell <= 0) continue
                    val cells = (0 until lastCell).map { c ->
                        cellText(row.getCell(c))
                    }
                    if (cells.any { it.isNotBlank() }) rows += cells
                }
                if (rows.isEmpty()) error("xls 工作表为空")
                rows
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("无法读取 xls：${e.message ?: e.javaClass.simpleName}", e)
        }
    }

    private fun cellText(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.BOOLEAN -> if (cell.booleanCellValue) "TRUE" else "FALSE"
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    formatter.formatCellValue(cell).trim()
                } else {
                    val n = cell.numericCellValue
                    if (n == n.toLong().toDouble()) n.toLong().toString() else formatter.formatCellValue(cell).trim()
                }
            }
            CellType.FORMULA -> runCatching {
                when (cell.cachedFormulaResultType) {
                    CellType.STRING -> cell.stringCellValue.trim()
                    CellType.NUMERIC -> {
                        val n = cell.numericCellValue
                        if (n == n.toLong().toDouble()) n.toLong().toString()
                        else formatter.formatCellValue(cell).trim()
                    }
                    CellType.BOOLEAN -> if (cell.booleanCellValue) "TRUE" else "FALSE"
                    else -> formatter.formatCellValue(cell).trim()
                }
            }.getOrElse { formatter.formatCellValue(cell).trim() }
            CellType.BLANK -> ""
            else -> formatter.formatCellValue(cell).trim()
        }
    }
}
