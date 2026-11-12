package com.attendance.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed class Cell {
    data class Text(val value: String, val header: Boolean = false) : Cell()
    data class Num(val value: Double) : Cell()
}

class SheetData(val name: String, val rows: List<List<Cell?>>, val widths: List<Int>)

/** Writes a real .xlsx file with no external library. */
object XlsxWriter {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    fun build(sheets: List<SheetData>): ByteArray {
        val names = uniqueNames(sheets)
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            fun put(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            put("[Content_Types].xml", contentTypes(sheets.size))
            put(
                "_rels/.rels",
                HEAD + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"$REL_NS/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>"
            )
            put("xl/workbook.xml", workbook(names))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/styles.xml", styles())
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
        return bos.toByteArray()
    }

    private fun contentTypes(count: Int): String {
        val sb = StringBuilder(HEAD)
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        for (i in 1..count) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun workbook(names: List<String>): String {
        val sb = StringBuilder(HEAD)
        sb.append("<workbook xmlns=\"$NS\" xmlns:r=\"$REL_NS\"><sheets>")
        names.forEachIndexed { i, n ->
            sb.append("<sheet name=\"${esc(n)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        sb.append("</sheets></workbook>")
        return sb.toString()
    }

    private fun workbookRels(count: Int): String {
        val sb = StringBuilder(HEAD)
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        for (i in 1..count) {
            sb.append("<Relationship Id=\"rId$i\" Type=\"$REL_NS/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        sb.append("<Relationship Id=\"rId${count + 1}\" Type=\"$REL_NS/styles\" Target=\"styles.xml\"/>")
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun styles(): String =
        HEAD + "<styleSheet xmlns=\"$NS\">" +
            "<fonts count=\"2\">" +
            "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "</fonts>" +
            "<fills count=\"3\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9E2F3\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "</fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"2\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

    private fun sheetXml(s: SheetData): String {
        val sb = StringBuilder(HEAD)
        sb.append("<worksheet xmlns=\"$NS\">")
        if (s.widths.isNotEmpty()) {
            sb.append("<cols>")
            s.widths.forEachIndexed { i, w ->
                sb.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
            }
            sb.append("</cols>")
        }
        sb.append("<sheetData>")
        s.rows.forEachIndexed { r, row ->
            sb.append("<row r=\"${r + 1}\">")
            row.forEachIndexed { c, cell ->
                val ref = colName(c) + (r + 1)
                when (cell) {
                    is Cell.Text -> {
                        val style = if (cell.header) " s=\"1\"" else ""
                        sb.append("<c r=\"$ref\" t=\"inlineStr\"$style><is><t xml:space=\"preserve\">${esc(cell.value)}</t></is></c>")
                    }
                    is Cell.Num -> sb.append("<c r=\"$ref\"><v>${cell.value}</v></c>")
                    null -> {}
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.insert(0, 'A' + r)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun esc(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 32 && ch != '\n' && ch != '\t' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun uniqueNames(sheets: List<SheetData>): List<String> {
        val used = HashSet<String>()
        return sheets.map { s ->
            val base = s.name.replace(Regex("[\\\\/?*\\[\\]:]"), " ").trim().trim('\'').take(28).ifEmpty { "Sheet" }
            var name = base
            var n = 2
            while (!used.add(name.lowercase())) {
                name = base.take(26) + " " + n
                n++
            }
            name
        }
    }
}

object Exporter {
    private const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val dateFmt = DateTimeFormatter.ofPattern("dd-MM-yy")

    fun mimeType(): String = MIME

    fun fileName(program: ClassProgram, sem: Int): String {
        val clean = program.name.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "Class" }
        return "Attendance_${clean}_Sem${sem}_${LocalDate.now()}.xlsx"
    }

    /** Builds one workbook: a Summary sheet plus one sheet for every subject. Returns null if no subjects. */
    fun build(store: Store, program: ClassProgram, sem: Int): ByteArray? {
        val subjects = store.subjects.filter { it.programId == program.id && it.semester == sem }
        if (subjects.isEmpty()) return null
        val students = store.students.filter { it.programId == program.id && it.semester == sem }

        val subjectSheets = ArrayList<SheetData>()
        val pctMaps = ArrayList<Map<String, Double>>()

        for (sub in subjects) {
            val recs = store.records.filter { it.subjectId == sub.id }
            val status = HashMap<String, String>()
            for (r in recs) status[r.studentId + "|" + r.date] = r.status
            val dates = recs.map { it.date }.distinct().sorted()
            val total = dates.size

            val rows = ArrayList<List<Cell?>>()
            rows.add(listOf(Cell.Text("Class: ${program.name}    Semester: $sem")))
            rows.add(listOf(Cell.Text("Subject: ${sub.name}    Teacher: ${store.teacherName(sub.teacherId)}")))
            rows.add(emptyList())

            val header = ArrayList<Cell?>()
            header.add(Cell.Text("Roll No", true))
            header.add(Cell.Text("Student Name", true))
            for (d in dates) header.add(Cell.Text(LocalDate.parse(d).format(dateFmt), true))
            for (h in listOf("Present", "Absent", "Leave", "Total Classes", "Percentage")) {
                header.add(Cell.Text(h, true))
            }
            rows.add(header)

            val pct = HashMap<String, Double>()
            for (st in students) {
                val row = ArrayList<Cell?>()
                row.add(Cell.Text(st.rollNo))
                row.add(Cell.Text(st.name))
                var p = 0
                var a = 0
                var l = 0
                for (d in dates) {
                    val s = status[st.id + "|" + d]
                    if (s != null) row.add(Cell.Text(s)) else row.add(null)
                    if (s == "P") p++
                    if (s == "A") a++
                    if (s == "L") l++
                }
                val percent = if (total == 0) 0.0 else Math.round(p * 1000.0 / total) / 10.0
                pct[st.id] = percent
                row.add(Cell.Num(p.toDouble()))
                row.add(Cell.Num(a.toDouble()))
                row.add(Cell.Num(l.toDouble()))
                row.add(Cell.Num(total.toDouble()))
                row.add(Cell.Num(percent))
                rows.add(row)
            }
            pctMaps.add(pct)

            val widths = ArrayList<Int>()
            widths.add(10)
            widths.add(28)
            for (d in dates) widths.add(10)
            widths.addAll(listOf(10, 10, 10, 14, 12))
            subjectSheets.add(SheetData(sub.name, rows, widths))
        }

        // Summary sheet
        val sumRows = ArrayList<List<Cell?>>()
        sumRows.add(listOf(Cell.Text("Class: ${program.name}    Semester: $sem    (Percentage per subject)")))
        sumRows.add(emptyList())
        val sumHeader = ArrayList<Cell?>()
        sumHeader.add(Cell.Text("Roll No", true))
        sumHeader.add(Cell.Text("Student Name", true))
        for (sub in subjects) sumHeader.add(Cell.Text(sub.name, true))
        sumHeader.add(Cell.Text("Average %", true))
        sumRows.add(sumHeader)
        for (st in students) {
            val row = ArrayList<Cell?>()
            row.add(Cell.Text(st.rollNo))
            row.add(Cell.Text(st.name))
            var sum = 0.0
            for (m in pctMaps) {
                val v = m[st.id] ?: 0.0
                sum += v
                row.add(Cell.Num(v))
            }
            row.add(Cell.Num(Math.round(sum * 10.0 / pctMaps.size) / 10.0))
            sumRows.add(row)
        }
        val sumWidths = ArrayList<Int>()
        sumWidths.add(10)
        sumWidths.add(28)
        for (sub in subjects) sumWidths.add(16)
        sumWidths.add(12)

        val all = ArrayList<SheetData>()
        all.add(SheetData("Summary", sumRows, sumWidths))
        all.addAll(subjectSheets)
        return XlsxWriter.build(all)
    }

    /** Saves into the phone's Downloads folder. Returns the file Uri or null on failure. */
    fun saveToDownloads(context: Context, fileName: String, bytes: ByteArray): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        val stream = resolver.openOutputStream(uri)
        if (stream == null) {
            resolver.delete(uri, null, null)
            return null
        }
        stream.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
