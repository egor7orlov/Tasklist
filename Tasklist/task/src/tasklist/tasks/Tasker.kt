package tasklist.tasks

import kotlinx.datetime.*
import tasklist.common.*
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.floor
import kotlin.math.roundToInt
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class Tasker {
    private val tasks = parseTasksFromFile().toMutableList()

    fun getTasksAmount(): Int = tasks.size

    fun tasksTable(): String {
        if (tasks.isEmpty()) {
            return "No tasks have been input"
        }

        return TasksTableDrawer.tasksTable(tasks)
    }

    fun addTask(
        priority: String,
        dateTime: LocalDateTime,
        subTasks: MutableList<String>,
    ) {
        this.tasks.add(
            Task(
                priority = priority, dateTimeString = dateTime.toString(), subTasks = subTasks, number = tasks.size + 1
            )
        )
    }

    fun deleteTask(taskNumber: Int) {
        tasks.removeAt(taskNumber - 1)
        // Updating tasks numbers after shift caused by element deletion
        tasks.mapIndexed { index, task -> task.number = index + 1 }
    }

    fun setTaskPriority(taskNumber: Int, priority: String) {
        getTaskByNumber(taskNumber).priority = priority
    }

    fun setTaskDate(taskNumber: Int, date: LocalDate) {
        getTaskByNumber(taskNumber).setDate(date)
    }

    fun setTaskTime(taskNumber: Int, time: LocalTime) {
        getTaskByNumber(taskNumber).setTime(time)
    }

    fun setTaskSubTasks(taskNumber: Int, subTasks: MutableList<String>) {
        getTaskByNumber(taskNumber).subTasks = subTasks
    }

    private fun getTaskByNumber(taskNumber: Int): Task {
        return tasks[taskNumber - 1]
    }

    fun endSession() {
        persistTasksInFile(tasks)
    }

    private companion object TasksJsonParser {
        private const val FILE_NAME = "tasklist.json"

        private fun parseTasksFromFile(): List<Task> {
            val tasksAdapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter<List<Task>>(Types.newParameterizedType(List::class.java, Task::class.java))
            val file = File(FILE_NAME)

            if (!file.exists()) {
                return listOf()
            }

            return tasksAdapter.fromJson(file.readText()) ?: listOf()
        }

        private fun persistTasksInFile(tasks: List<Task>) {
            val tasksAdapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter<List<Task>>(Types.newParameterizedType(List::class.java, Task::class.java))

            File(FILE_NAME).writeText(tasksAdapter.toJson(tasks))
        }
    }
}

private class Task(
    var priority: String, var dateTimeString: String, var subTasks: MutableList<String>, var number: Int
) {
    fun setDate(date: LocalDate) {
        this.dateTimeString = LocalDateTime.parse(this.dateTimeString)
            .withDayOfMonth(date.dayOfMonth)
            .withMonth(date.monthNumber)
            .withYear(date.year)
            .toString()
    }

    fun setTime(time: LocalTime) {
        this.dateTimeString = LocalDateTime.parse(this.dateTimeString)
            .withHour(time.hour)
            .withMinute(time.minute)
            .toString()
    }

    fun getStatusColor(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0"))
        val numberOfDays =
            now.date.daysUntil(LocalDate.parse(LocalDateTime.parse(dateTimeString).toLocalDate().toString()))
        val isInTime = numberOfDays > 0
        val isOverdue = numberOfDays < 0

        return when {
            isInTime -> GREEN_ESC_SEQ
            isOverdue -> RED_ESC_SEQ
            else -> YELLOW_ESC_SEQ
        }
    }

    fun getPriorityColor(): String {
        return when (priority) {
            CRITICAL -> RED_ESC_SEQ
            HIGH -> YELLOW_ESC_SEQ
            NORMAL -> GREEN_ESC_SEQ
            LOW -> BLUE_ESC_SEQ
            else -> {
                throw Exception("Unknown task priority")
            }
        }
    }

    companion object Priority {
        const val CRITICAL = "C"
        const val HIGH = "H"
        const val NORMAL = "N"
        const val LOW = "L"
    }
}

// I'm not proud of this class.
// I just wanted to complete the task and don't spend too much time organizing methods -__-
private class TasksTableDrawer {
    companion object {
        private const val NUMBER_COL_WIDTH = 4
        private const val DATE_COL_WIDTH = 12
        private const val TIME_COL_WIDTH = 7
        private const val PRIORITY_COL_WIDTH = 3
        private const val STATUS_COL_WIDTH = 3
        private const val TASK_COL_WIDTH = 44

        fun tasksTable(tasks: List<Task>): String {
            var result = "${sep()}\n${header()}\n${sep()}"

            tasks.forEach {
                result += "\n${
                    getRow(
                        number = it.number.toString(),
                        date = LocalDateTime.parse(it.dateTimeString).toLocalDate().toString(),
                        time = LocalDateTime.parse(it.dateTimeString).toLocalTime().toString(),
                        priority = it.getPriorityColor(),
                        status = it.getStatusColor(),
                        task = it.subTasks.joinToString("\n"),
                    )
                }"
                result += "\n${sep()}"
            }

            return result
        }

        private fun header(): String {
            val numberCell = paddedCell("N", NUMBER_COL_WIDTH)
            val dateCell = paddedCell("Date", DATE_COL_WIDTH)
            val timeCell = paddedCell("Time", TIME_COL_WIDTH)
            val priorityCell = paddedCell("P", PRIORITY_COL_WIDTH)
            val statusCell = paddedCell("D", STATUS_COL_WIDTH)
            // Example's "Task" header has next layout: "{19 spaces}Task{21 spaces}".
            // It makes no sense for header title to be so asymmetrical since header
            // of width 44 can easily be: "{20 spaces}Task{20 spaces}".
            // Also, such nonsense should be stated in task's description.
            //val taskCell = paddedCell("Task", TASK_COL_WIDTH)
            val taskCell = "${space(19)}Task${space(21)}"

            return "|$numberCell|$dateCell|$timeCell|$priorityCell|$statusCell|$taskCell|"
        }

        private fun getRow(
            number: String,
            date: String,
            time: String,
            priority: String,
            status: String,
            task: String
        ): String {
            val numberCell = paddedCell(number, NUMBER_COL_WIDTH)
            val dateCell = paddedCell(date, DATE_COL_WIDTH)
            val timeCell = paddedCell(time, TIME_COL_WIDTH)
            val priorityCell = paddedCell(priority, PRIORITY_COL_WIDTH, ignoreTextWidth = true)
            val statusCell = paddedCell(status, STATUS_COL_WIDTH, ignoreTextWidth = true)
            val taskLines = constrainedTaskLines(task, TASK_COL_WIDTH)
            var row =
                "|$numberCell|$dateCell|$timeCell|$priorityCell|$statusCell|${taskLines.first() + space(TASK_COL_WIDTH - taskLines.first().length)}|"

            if (taskLines.size > 1) {
                taskLines.subList(1, taskLines.lastIndex + 1).forEach {
                    row += "\n|${space(NUMBER_COL_WIDTH)}|${space(DATE_COL_WIDTH)}|${space(TIME_COL_WIDTH)}|${
                        space(
                            PRIORITY_COL_WIDTH
                        )
                    }|${space(STATUS_COL_WIDTH)}|${it + space(TASK_COL_WIDTH - it.length)}|"
                }
            }

            return row
        }

        fun constrainedTaskLines(text: String, width: Int): List<String> {
            return text.split("\n")
                .filter { it.isNotEmpty() }
                .fold(mutableListOf()) { acc, s ->
                    val transferredLines = mutableListOf<String>()

                    for (i in 0 until (s.length / width)) {
                        val constrainedLine = s.substring((i * width) until (i * width + width))

                        transferredLines.add(constrainedLine)
                    }

                    if (s.length % width != 0) {
                        val remainingLine = s.substring(s.length - s.length % width)

                        transferredLines.add(remainingLine)
                    }

                    acc.addAll(transferredLines)
                    acc
                }
        }

        fun paddedCell(text: String, width: Int, ignoreTextWidth: Boolean = false): String {
            val lengthDiff = if (ignoreTextWidth) {
                width
            } else {
                width - text.length
            }
            val leftPadWidth = floor(lengthDiff / 2.0).roundToInt()
            val rightPadWidth = (if (ignoreTextWidth || lengthDiff % 2 == 0) {
                leftPadWidth
            } else {
                leftPadWidth + 1
            })

            return "${space(leftPadWidth)}${text}${space(rightPadWidth)}"
        }

        fun space(width: Int) = " ".repeat(width)

        fun sep() = "+${"-".repeat(NUMBER_COL_WIDTH)}+${"-".repeat(DATE_COL_WIDTH)}+${"-".repeat(TIME_COL_WIDTH)}+${
            "-".repeat(PRIORITY_COL_WIDTH)
        }+${"-".repeat(STATUS_COL_WIDTH)}+${"-".repeat(TASK_COL_WIDTH)}+"
    }
}
