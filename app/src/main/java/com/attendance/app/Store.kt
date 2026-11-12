package com.attendance.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ClassProgram(val id: String, val name: String, val semesterCount: Int)
data class Teacher(val id: String, val name: String)
data class Subject(
    val id: String,
    val programId: String,
    val semester: Int,
    val name: String,
    val teacherId: String
)
data class Student(
    val id: String,
    val programId: String,
    val semester: Int,
    val rollNo: String,
    val name: String
)
/** status is "P" (present), "A" (absent) or "L" (leave). date is ISO like 2026-11-12. */
data class AttRecord(
    val subjectId: String,
    val studentId: String,
    val date: String,
    val status: String
)

class Store(context: Context) {
    val programs = mutableStateListOf<ClassProgram>()
    val teachers = mutableStateListOf<Teacher>()
    val subjects = mutableStateListOf<Subject>()
    val students = mutableStateListOf<Student>()
    val records = mutableStateListOf<AttRecord>()

    private val file = File(context.filesDir, "attendance_data.json")

    init {
        load()
    }

    private fun newId(): String = UUID.randomUUID().toString()

    // ---------- Classes ----------
    fun addProgram(name: String, semesters: Int) {
        programs.add(ClassProgram(newId(), name.trim(), semesters.coerceIn(1, 12)))
        save()
    }

    fun deleteProgram(id: String) {
        val subjectIds = subjects.filter { it.programId == id }.map { it.id }.toSet()
        val studentIds = students.filter { it.programId == id }.map { it.id }.toSet()
        records.removeAll { it.subjectId in subjectIds || it.studentId in studentIds }
        subjects.removeAll { it.programId == id }
        students.removeAll { it.programId == id }
        programs.removeAll { it.id == id }
        save()
    }

    // ---------- Teachers ----------
    fun addTeacher(name: String) {
        teachers.add(Teacher(newId(), name.trim()))
        save()
    }

    fun deleteTeacher(id: String) {
        for (i in subjects.indices) {
            if (subjects[i].teacherId == id) subjects[i] = subjects[i].copy(teacherId = "")
        }
        teachers.removeAll { it.id == id }
        save()
    }

    fun teacherName(id: String): String =
        teachers.firstOrNull { it.id == id }?.name ?: "No teacher"

    // ---------- Subjects ----------
    fun addSubject(programId: String, semester: Int, name: String, teacherId: String) {
        subjects.add(Subject(newId(), programId, semester, name.trim(), teacherId))
        save()
    }

    fun deleteSubject(id: String) {
        records.removeAll { it.subjectId == id }
        subjects.removeAll { it.id == id }
        save()
    }

    // ---------- Students ----------
    /** Each pair is (rollNo, name). */
    fun addStudents(programId: String, semester: Int, list: List<Pair<String, String>>) {
        for ((roll, name) in list) {
            students.add(Student(newId(), programId, semester, roll, name))
        }
        save()
    }

    fun deleteStudent(id: String) {
        records.removeAll { it.studentId == id }
        students.removeAll { it.id == id }
        save()
    }

    // ---------- Attendance ----------
    fun existing(subjectId: String, date: String): Map<String, String> =
        records.filter { it.subjectId == subjectId && it.date == date }
            .associate { it.studentId to it.status }

    fun saveAttendance(subjectId: String, date: String, statuses: Map<String, String>) {
        records.removeAll { it.subjectId == subjectId && it.date == date }
        records.addAll(statuses.map { (studentId, status) ->
            AttRecord(subjectId, studentId, date, status)
        })
        save()
    }

    // ---------- Storage ----------
    private fun save() {
        try {
            val root = JSONObject()
            root.put("programs", JSONArray().apply {
                programs.forEach {
                    put(JSONObject().put("id", it.id).put("name", it.name).put("semesterCount", it.semesterCount))
                }
            })
            root.put("teachers", JSONArray().apply {
                teachers.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
            })
            root.put("subjects", JSONArray().apply {
                subjects.forEach {
                    put(
                        JSONObject().put("id", it.id).put("programId", it.programId)
                            .put("semester", it.semester).put("name", it.name)
                            .put("teacherId", it.teacherId)
                    )
                }
            })
            root.put("students", JSONArray().apply {
                students.forEach {
                    put(
                        JSONObject().put("id", it.id).put("programId", it.programId)
                            .put("semester", it.semester).put("rollNo", it.rollNo)
                            .put("name", it.name)
                    )
                }
            })
            root.put("records", JSONArray().apply {
                records.forEach {
                    put(
                        JSONObject().put("s", it.subjectId).put("st", it.studentId)
                            .put("d", it.date).put("v", it.status)
                    )
                }
            })
            val text = root.toString()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            // Saving is best effort; the app keeps working with in-memory data.
        }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            root.optJSONArray("programs")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    programs.add(ClassProgram(o.getString("id"), o.getString("name"), o.getInt("semesterCount")))
                }
            }
            root.optJSONArray("teachers")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    teachers.add(Teacher(o.getString("id"), o.getString("name")))
                }
            }
            root.optJSONArray("subjects")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    subjects.add(
                        Subject(
                            o.getString("id"), o.getString("programId"), o.getInt("semester"),
                            o.getString("name"), o.getString("teacherId")
                        )
                    )
                }
            }
            root.optJSONArray("students")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    students.add(
                        Student(
                            o.getString("id"), o.getString("programId"), o.getInt("semester"),
                            o.getString("rollNo"), o.getString("name")
                        )
                    )
                }
            }
            root.optJSONArray("records")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    records.add(AttRecord(o.getString("s"), o.getString("st"), o.getString("d"), o.getString("v")))
                }
            }
        } catch (e: Exception) {
            // Corrupt file: start empty rather than crash.
        }
    }
}
