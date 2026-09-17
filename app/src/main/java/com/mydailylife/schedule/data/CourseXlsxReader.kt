package com.mydailylife.schedule.data

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal first-sheet reader for `.xlsx` (Office Open XML).
 * Enough for tabular course imports; does not evaluate formulas.
 */
internal object CourseXlsxReader {
    fun readSheetRows(bytes: ByteArray): List<List<String>> {
        val parts = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            val map = mutableMapOf<String, ByteArray>()
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (
                    name.equals("xl/sharedStrings.xml", ignoreCase = true) ||
                    name.equals("xl/workbook.xml", ignoreCase = true) ||
                    name.matches(Regex("""xl/worksheets/sheet\d+\.xml""", RegexOption.IGNORE_CASE))
                ) {
                    map[name.lowercase()] = zip.readBytes()
                }
                zip.closeEntry()
            }
            map
        }
        if (parts.isEmpty()) error("无法读取 xlsx（可能已损坏或不是有效工作簿）")

        val shared = parts["xl/sharedstrings.xml"]?.let { parseSharedStrings(it) }.orEmpty()
        val sheetBytes = parts["xl/worksheets/sheet1.xml"]
            ?: parts.keys.firstOrNull { it.startsWith("xl/worksheets/sheet") }?.let { parts[it] }
            ?: error("xlsx 中没有工作表")
        return parseSheet(sheetBytes, shared)
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val root = parseXml(xml)
        val out = mutableListOf<String>()
        for (si in elementsByLocalName(root, "si")) {
            out += collectText(si)
        }
        return out
    }

    private fun parseSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val root = parseXml(xml)
        val rows = mutableListOf<List<String>>()
        for (rowEl in elementsByLocalName(root, "row")) {
            val cells = mutableMapOf<Int, String>()
            var maxCol = -1
            for (cell in elementsByLocalName(rowEl, "c")) {
                val ref = cell.getAttribute("r")
                val col = columnIndex(ref)
                if (col < 0) continue
                val type = cell.getAttribute("t")
                val raw = firstChildText(cell, "v")
                    ?: firstChildText(cell, "t")
                    ?: ""
                val value = when (type) {
                    "s" -> shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                    "inlineStr", "str" -> raw.ifEmpty { collectText(cell) }
                    "b" -> if (raw == "1") "TRUE" else "FALSE"
                    else -> trimNumeric(raw)
                }
                cells[col] = value
                if (col > maxCol) maxCol = col
            }
            if (maxCol >= 0) {
                rows += (0..maxCol).map { cells[it].orEmpty() }
            }
        }
        return rows
    }

    private fun parseXml(bytes: ByteArray): Element {
        // Drop xmlns so JVM unit tests and on-device parsers agree on local tag names.
        val stripped = bytes.toString(Charsets.UTF_8)
            .replace(Regex("""\sxmlns(:\w+)?="[^"]*""""), "")
            .toByteArray(Charsets.UTF_8)
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(stripped))
        return doc.documentElement
    }

    private fun elementsByLocalName(root: Element, localName: String): List<Element> {
        val nodes = root.getElementsByTagName(localName)
        return buildList {
            for (i in 0 until nodes.length) {
                add(nodes.item(i) as Element)
            }
        }
    }

    private fun collectText(el: Element): String {
        val sb = StringBuilder()
        fun walk(node: Node) {
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> sb.append(node.nodeValue)
                Node.ELEMENT_NODE -> {
                    val children = node.childNodes
                    for (i in 0 until children.length) walk(children.item(i))
                }
            }
        }
        walk(el)
        return sb.toString()
    }

    private fun firstChildText(parent: Element, tag: String): String? {
        val nodes = parent.getElementsByTagName(tag)
        if (nodes.length == 0) return null
        for (i in 0 until nodes.length) {
            val n = nodes.item(i)
            if (n.parentNode === parent) return n.textContent
        }
        return nodes.item(0)?.textContent
    }

    /** A1 → 0, B1 → 1, AA2 → 26 */
    private fun columnIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }
        if (letters.isEmpty()) return -1
        var n = 0
        for (ch in letters.uppercase()) {
            n = n * 26 + (ch - 'A' + 1)
        }
        return n - 1
    }

    private fun trimNumeric(raw: String): String {
        val d = raw.toDoubleOrNull() ?: return raw
        return if (d == d.toLong().toDouble()) d.toLong().toString() else raw
    }
}
