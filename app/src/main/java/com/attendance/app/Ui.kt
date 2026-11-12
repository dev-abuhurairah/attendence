package com.attendance.app

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------- Theme

val Periwinkle = Color(0xFF6495ED)
val Navy = Color(0xFF002B5B)
val ScreenBg = Color(0xFFF5F8FF)
private val Green = Color(0xFF2E9E5B)
private val Red = Color(0xFFD64545)
private val Amber = Color(0xFFE0A100)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Periwinkle,
            onPrimary = Navy,
            background = ScreenBg,
            onBackground = Navy,
            surface = Color.White,
            onSurface = Navy
        ),
        content = content
    )
}

// ---------------------------------------------------------------- Navigation

sealed interface Screen {
    data object Home : Screen
    data object Setup : Screen
    data object SetupClasses : Screen
    data object SetupTeachers : Screen
    data object SetupSubjects : Screen
    data object SetupStudents : Screen
    data class Semesters(val programId: String) : Screen
    data class SemesterMenu(val programId: String, val sem: Int) : Screen
    data class PickSubject(val programId: String, val sem: Int) : Screen
    data class Mark(val programId: String, val sem: Int, val subjectId: String) : Screen
    data class Report(val programId: String, val sem: Int) : Screen
}

@Composable
fun App(store: Store) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.size > 1) { back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
    ) {
        when (val s = stack.last()) {
            is Screen.Home -> HomeScreen(store, go)
            is Screen.Setup -> SetupScreen(go, back)
            is Screen.SetupClasses -> ClassesSetup(store, back)
            is Screen.SetupTeachers -> TeachersSetup(store, back)
            is Screen.SetupSubjects -> SubjectsSetup(store, back)
            is Screen.SetupStudents -> StudentsSetup(store, back)
            is Screen.Semesters -> SemestersScreen(store, s.programId, go, back)
            is Screen.SemesterMenu -> SemesterMenuScreen(store, s.programId, s.sem, go, back)
            is Screen.PickSubject -> PickSubjectScreen(store, s.programId, s.sem, go, back)
            is Screen.Mark -> MarkScreen(store, s.programId, s.sem, s.subjectId, back)
            is Screen.Report -> ReportScreen(store, s.programId, s.sem, back)
        }
    }
}

// ---------------------------------------------------------------- Shared pieces

/** The main button used across the app: big, flat, fully rounded pill. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(110.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Periwinkle,
            contentColor = Navy
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 28.dp)
    ) {
        Text(
            text = text,
            color = Navy,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ScreenFrame(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text("Back", fontSize = 18.sp, color = Navy)
                }
            }
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = if (onBack != null) 4.dp else 16.dp)
            )
        }
        content()
    }
}

@Composable
fun ButtonList(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}

@Composable
fun Hint(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Navy.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    )
}

@Composable
fun ItemRow(title: String, subtitle: String = "", onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Navy)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 14.sp, color = Navy.copy(alpha = 0.65f))
            }
        }
        TextButton(onClick = onDelete) {
            Text("Delete", color = Red)
        }
    }
}

@Composable
fun FormDialog(
    title: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmDelete(message: String, onYes: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete?") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onYes) { Text("Delete", color = Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun Dropdown(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (selected in options.indices) options[selected] else label,
                color = Navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = {
                        onSelect(i)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
fun ClassPicker(
    store: Store,
    pIdx: Int,
    onProgram: (Int) -> Unit,
    sem: Int,
    onSem: (Int) -> Unit
) {
    val program = store.programs.getOrNull(pIdx)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Dropdown(
            "Class",
            store.programs.map { it.name },
            pIdx,
            onProgram,
            Modifier.weight(1f)
        )
        Dropdown(
            "Semester",
            (1..(program?.semesterCount ?: 1)).map { "Semester $it" },
            sem - 1,
            { onSem(it + 1) },
            Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------- Home and setup

@Composable
fun HomeScreen(store: Store, go: (Screen) -> Unit) {
    ScreenFrame("Student Attendance", null) {
        ButtonList {
            PillButton("Setup") { go(Screen.Setup) }
            for (p in store.programs) {
                PillButton(p.name) { go(Screen.Semesters(p.id)) }
            }
            if (store.programs.isEmpty()) {
                Hint("Tap Setup and add your first class, for example BBA.")
            }
        }
    }
}

@Composable
fun SetupScreen(go: (Screen) -> Unit, back: () -> Unit) {
    ScreenFrame("Setup", back) {
        ButtonList {
            PillButton("Classes") { go(Screen.SetupClasses) }
            PillButton("Teachers") { go(Screen.SetupTeachers) }
            PillButton("Subjects") { go(Screen.SetupSubjects) }
            PillButton("Students") { go(Screen.SetupStudents) }
        }
    }
}

@Composable
fun ClassesSetup(store: Store, back: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ClassProgram?>(null) }

    ScreenFrame("Classes", back) {
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item { PillButton("Add Class") { showAdd = true } }
            items(store.programs, key = { it.id }) { p ->
                ItemRow(p.name, "${p.semesterCount} semesters") { toDelete = p }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var count by remember { mutableStateOf("8") }
        FormDialog(
            title = "Add Class",
            confirmEnabled = name.isNotBlank() && (count.toIntOrNull() ?: 0) in 1..12,
            onDismiss = { showAdd = false },
            onConfirm = {
                store.addProgram(name, count.toInt())
                showAdd = false
            }
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Class name (for example BBA)") },
                singleLine = true
            )
            OutlinedTextField(
                value = count,
                onValueChange = { count = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Number of semesters") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }

    toDelete?.let { p ->
        ConfirmDelete(
            "Delete ${p.name} with all its subjects, students and attendance?",
            onYes = {
                store.deleteProgram(p.id)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
fun TeachersSetup(store: Store, back: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Teacher?>(null) }

    ScreenFrame("Teachers", back) {
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item { PillButton("Add Teacher") { showAdd = true } }
            items(store.teachers, key = { it.id }) { t ->
                ItemRow(t.name) { toDelete = t }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        FormDialog(
            title = "Add Teacher",
            confirmEnabled = name.isNotBlank(),
            onDismiss = { showAdd = false },
            onConfirm = {
                store.addTeacher(name)
                showAdd = false
            }
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Teacher name") },
                singleLine = true
            )
        }
    }

    toDelete?.let { t ->
        ConfirmDelete(
            "Delete teacher ${t.name}? Their subjects will show No teacher.",
            onYes = {
                store.deleteTeacher(t.id)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
fun SubjectsSetup(store: Store, back: () -> Unit) {
    var pIdx by remember { mutableIntStateOf(0) }
    var sem by remember { mutableIntStateOf(1) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Subject?>(null) }
    val program = store.programs.getOrNull(pIdx)

    ScreenFrame("Subjects", back) {
        if (program == null) {
            Hint("Add a class first in Setup > Classes.")
        } else {
            ClassPicker(store, pIdx, { pIdx = it; sem = 1 }, sem, { sem = it })
            val list = store.subjects.filter { it.programId == program.id && it.semester == sem }
            LazyColumn(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item { PillButton("Add Subject") { showAdd = true } }
                items(list, key = { it.id }) { s ->
                    ItemRow(s.name, "Teacher: ${store.teacherName(s.teacherId)}") { toDelete = s }
                }
            }
        }
    }

    if (showAdd && program != null) {
        var name by remember { mutableStateOf("") }
        var tIdx by remember { mutableIntStateOf(0) }
        FormDialog(
            title = "Add Subject",
            confirmEnabled = name.isNotBlank(),
            onDismiss = { showAdd = false },
            onConfirm = {
                store.addSubject(program.id, sem, name, store.teachers.getOrNull(tIdx)?.id ?: "")
                showAdd = false
            }
        ) {
            Text("${program.name} - Semester $sem", color = Navy.copy(alpha = 0.7f))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subject name") },
                singleLine = true
            )
            if (store.teachers.isEmpty()) {
                Text("No teachers yet. Add teachers in Setup > Teachers, then add the subject again to pick one.")
            } else {
                Dropdown("Teacher", store.teachers.map { it.name }, tIdx, { tIdx = it }, Modifier.fillMaxWidth())
            }
        }
    }

    toDelete?.let { s ->
        ConfirmDelete(
            "Delete ${s.name} and its attendance?",
            onYes = {
                store.deleteSubject(s.id)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
fun StudentsSetup(store: Store, back: () -> Unit) {
    var pIdx by remember { mutableIntStateOf(0) }
    var sem by remember { mutableIntStateOf(1) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Student?>(null) }
    val program = store.programs.getOrNull(pIdx)

    ScreenFrame("Students", back) {
        if (program == null) {
            Hint("Add a class first in Setup > Classes.")
        } else {
            ClassPicker(store, pIdx, { pIdx = it; sem = 1 }, sem, { sem = it })
            val list = store.students.filter { it.programId == program.id && it.semester == sem }
            LazyColumn(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item { PillButton("Add Students") { showAdd = true } }
                items(list, key = { it.id }) { st ->
                    ItemRow(st.name, "Roll No: ${st.rollNo}") { toDelete = st }
                }
            }
        }
    }

    if (showAdd && program != null) {
        var text by remember { mutableStateOf("") }
        FormDialog(
            title = "Add Students",
            confirmEnabled = text.isNotBlank(),
            onDismiss = { showAdd = false },
            onConfirm = {
                val existing = store.students.count { it.programId == program.id && it.semester == sem }
                val parsed = ArrayList<Pair<String, String>>()
                text.lines().filter { it.isNotBlank() }.forEachIndexed { i, line ->
                    val comma = line.indexOf(',')
                    val roll = if (comma > 0) line.substring(0, comma).trim() else (existing + i + 1).toString()
                    val name = if (comma > 0) line.substring(comma + 1).trim() else line.trim()
                    if (name.isNotEmpty()) parsed.add(Pair(roll, name))
                }
                store.addStudents(program.id, sem, parsed)
                showAdd = false
            }
        ) {
            Text("${program.name} - Semester $sem", color = Navy.copy(alpha = 0.7f))
            Text("Write one student on each line. Use Roll No, Name  or just the Name.", fontSize = 14.sp)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Students") },
                placeholder = { Text("101, Ali Khan\n102, Sara Ahmed") },
                minLines = 5
            )
        }
    }

    toDelete?.let { st ->
        ConfirmDelete(
            "Delete ${st.name} and their attendance?",
            onYes = {
                store.deleteStudent(st.id)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

// ---------------------------------------------------------------- Home flow

@Composable
fun SemestersScreen(store: Store, programId: String, go: (Screen) -> Unit, back: () -> Unit) {
    val p = store.programs.firstOrNull { it.id == programId }
    ScreenFrame(p?.name ?: "Class", back) {
        ButtonList {
            if (p != null) {
                for (i in 1..p.semesterCount) {
                    PillButton("Semester $i") { go(Screen.SemesterMenu(p.id, i)) }
                }
            }
        }
    }
}

@Composable
fun SemesterMenuScreen(store: Store, programId: String, sem: Int, go: (Screen) -> Unit, back: () -> Unit) {
    val p = store.programs.firstOrNull { it.id == programId }
    ScreenFrame("${p?.name ?: "Class"} - Semester $sem", back) {
        ButtonList {
            PillButton("Attendance") { go(Screen.PickSubject(programId, sem)) }
            PillButton("Report & Excel") { go(Screen.Report(programId, sem)) }
        }
    }
}

@Composable
fun PickSubjectScreen(store: Store, programId: String, sem: Int, go: (Screen) -> Unit, back: () -> Unit) {
    val list = store.subjects.filter { it.programId == programId && it.semester == sem }
    ScreenFrame("Choose Subject", back) {
        ButtonList {
            if (list.isEmpty()) {
                Hint("No subjects yet. Add them in Setup > Subjects.")
            }
            for (s in list) {
                PillButton(s.name) { go(Screen.Mark(programId, sem, s.id)) }
                Text(
                    "Teacher: ${store.teacherName(s.teacherId)}",
                    fontSize = 14.sp,
                    color = Navy.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Attendance

@Composable
fun StatusChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .width(46.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else Color(0xFFE8ECF4))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else Navy
        )
    }
}

@Composable
fun MarkScreen(store: Store, programId: String, sem: Int, subjectId: String, back: () -> Unit) {
    val context = LocalContext.current
    val subject = store.subjects.firstOrNull { it.id == subjectId }
    val students = store.students.filter { it.programId == programId && it.semester == sem }
    var date by remember { mutableStateOf(LocalDate.now()) }
    val dateText = date.toString()
    val statuses = remember(subjectId, date) {
        val saved = store.existing(subjectId, date.toString())
        val m = mutableStateMapOf<String, String>()
        for (st in students) m[st.id] = saved[st.id] ?: "P"
        m
    }
    val alreadySaved = store.records.any { it.subjectId == subjectId && it.date == dateText }

    ScreenFrame(subject?.name ?: "Attendance", back) {
        if (subject == null) {
            Hint("Subject not found.")
        } else {
            Text(
                "Teacher: ${store.teacherName(subject.teacherId)}",
                fontSize = 15.sp,
                color = Navy.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), color = Navy)
                }
                TextButton(onClick = {
                    for (st in students) statuses[st.id] = "P"
                }) {
                    Text("All present", color = Navy)
                }
            }
            val p = statuses.values.count { it == "P" }
            val a = statuses.values.count { it == "A" }
            val l = statuses.values.count { it == "L" }
            Text(
                "Present $p    Absent $a    Leave $l" + if (alreadySaved) "    (saved before)" else "",
                fontSize = 14.sp,
                color = Navy,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (students.isEmpty()) {
                Hint("No students in this semester. Add them in Setup > Students.")
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students, key = { it.id }) { st ->
                        val cur = statuses[st.id] ?: "P"
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    st.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Navy,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Roll ${st.rollNo}", fontSize = 13.sp, color = Navy.copy(alpha = 0.65f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusChip("P", cur == "P", Green) { statuses[st.id] = "P" }
                                StatusChip("A", cur == "A", Red) { statuses[st.id] = "A" }
                                StatusChip("L", cur == "L", Amber) { statuses[st.id] = "L" }
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PillButton("Save") {
                        store.saveAttendance(subjectId, dateText, statuses.toMap())
                        Toast.makeText(context, "Attendance saved", Toast.LENGTH_SHORT).show()
                        back()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Report and Excel

@Composable
fun ReportScreen(store: Store, programId: String, sem: Int, back: () -> Unit) {
    val context = LocalContext.current
    val program = store.programs.firstOrNull { it.id == programId }
    val subjects = store.subjects.filter { it.programId == programId && it.semester == sem }
    val students = store.students.filter { it.programId == programId && it.semester == sem }
    var sIdx by remember { mutableIntStateOf(0) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var savedName by remember { mutableStateOf("") }

    ScreenFrame("Report", back) {
        if (program == null || subjects.isEmpty()) {
            Hint("No subjects in this semester yet. Add them in Setup > Subjects.")
        } else {
            val sub = subjects[sIdx.coerceIn(0, subjects.lastIndex)]
            Dropdown(
                "Subject",
                subjects.map { it.name },
                sIdx,
                { sIdx = it },
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val recs = store.records.filter { it.subjectId == sub.id }
            val total = recs.map { it.date }.distinct().size
            Text(
                "Classes taken: $total",
                fontSize = 15.sp,
                color = Navy,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Student", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Navy)
                Text("P", Modifier.width(34.dp), fontWeight = FontWeight.SemiBold, color = Navy)
                Text("A", Modifier.width(34.dp), fontWeight = FontWeight.SemiBold, color = Navy)
                Text("L", Modifier.width(34.dp), fontWeight = FontWeight.SemiBold, color = Navy)
                Text("%", Modifier.width(52.dp), fontWeight = FontWeight.SemiBold, color = Navy)
            }
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(students, key = { it.id }) { st ->
                    val mine = recs.filter { it.studentId == st.id }
                    val p = mine.count { it.status == "P" }
                    val a = mine.count { it.status == "A" }
                    val l = mine.count { it.status == "L" }
                    val pct = if (total == 0) 0.0 else Math.round(p * 1000.0 / total) / 10.0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${st.rollNo}  ${st.name}",
                            Modifier.weight(1f),
                            color = Navy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("$p", Modifier.width(34.dp), color = Navy)
                        Text("$a", Modifier.width(34.dp), color = Navy)
                        Text("$l", Modifier.width(34.dp), color = Navy)
                        Text(
                            "$pct",
                            Modifier.width(52.dp),
                            color = if (total > 0 && pct < 75.0) Red else Navy,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                PillButton("Create Excel") {
                    try {
                        val bytes = Exporter.build(store, program, sem)
                        if (bytes == null) {
                            Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val name = Exporter.fileName(program, sem)
                            val uri = Exporter.saveToDownloads(context, name, bytes)
                            if (uri == null) {
                                Toast.makeText(context, "Could not save the file", Toast.LENGTH_LONG).show()
                            } else {
                                savedName = name
                                savedUri = uri
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    savedUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { savedUri = null },
            title = { Text("Excel file saved") },
            text = { Text("$savedName\n\nSaved in your Downloads folder.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            val open = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, Exporter.mimeType())
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(open)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No app found to open Excel files", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("Open") }
                    TextButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = Exporter.mimeType()
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "Share Excel file"))
                    }) { Text("Share") }
                }
            },
            dismissButton = {
                TextButton(onClick = { savedUri = null }) { Text("Close") }
            }
        )
    }
}
