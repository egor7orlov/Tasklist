package tasklist.commands

import tasklist.tasks.Tasker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Asker(private val tasker: Tasker) {
    fun initDialogue() {
        commandPollingLoop@ while (true) {
            println("Input an action (add, print, edit, delete, end):")

            when (readln().lowercase()) {
                "end" -> {
                    println("Tasklist exiting!")
                    tasker.endSession()
                    break@commandPollingLoop
                }

                "add" -> startTasksAddition()

                "edit" -> startTasksEditing()

                "delete" -> startTasksDeletion()

                "print" -> printFormattedTasks()

                else -> {
                    println("The input action is invalid")
                }
            }
        }
    }

    private fun startTasksAddition() {
        val priority = askUserForTaskPriority()
        val dateTime = askUserForDateTime()
        val subTasks = askUserForSubTasks() ?: return

        this.tasker.addTask(
            priority = priority,
            dateTime = dateTime,
            subTasks = subTasks,
        )
    }

    private fun askUserForSubTasks(): MutableList<String>? {
        println("Input a new task (enter a blank line to end):")

        val subTasks = mutableListOf<String>()

        tasksAdditionLoop@ while (true) {
            val input = readln().trim()

            when {
                input.isEmpty() -> {
                    break@tasksAdditionLoop
                }

                else -> {
                    subTasks.add(input)
                }
            }
        }

        if (subTasks.isEmpty()) {
            println("The task is blank")
            return null
        }

        return subTasks
    }

    private fun askUserForTaskPriority(): String {
        val possiblePriorities = listOf("C", "H", "N", "L")

        while (true) {
            println("Input the task priority (${possiblePriorities.joinToString(", ")}):")

            val inputPriority = readln().uppercase()

            if (inputPriority in possiblePriorities) {
                return inputPriority
            }
        }
    }

    private fun askUserForDateTime(): LocalDateTime {
        val date = askUserForDate()

        return askUserForTime().atDate(date)
    }

    private fun askUserForDate(): LocalDate {
        while (true) {
            println("Input the date (yyyy-mm-dd):")

            try {
                val (year, month, day) = readln().split("-").map { it.toInt() }
                return LocalDate.of(year, month, day)
            } catch (err: Exception) {
                println("The input date is invalid")
                continue
            }
        }
    }

    private fun askUserForTime(): LocalTime {
        while (true) {
            println("Input the time (hh:mm):")

            try {
                val (hours, minutes) = readln().split(":").map { it.toInt() }
                return LocalTime.of(hours, minutes)
            } catch (e: Exception) {
                println("The input time is invalid")
            }
        }
    }

    private fun startTasksEditing() {
        val tasksAmount = tasker.getTasksAmount()

        if (tasksAmount <= 0) {
            println("No tasks have been input")
            return
        }

        printFormattedTasks()

        val taskNumber = askForUserTaskNumber(tasksAmount)

        when (askUserForFieldToEditName()) {
            "priority" -> tasker.setTaskPriority(taskNumber, askUserForTaskPriority())
            "date" -> tasker.setTaskDate(taskNumber, kotlinx.datetime.LocalDate.parse(askUserForDate().toString()))
            "time" -> tasker.setTaskTime(taskNumber, askUserForTime())
            "task" -> {
                val subTasks = askUserForSubTasks() ?: return

                tasker.setTaskSubTasks(taskNumber, subTasks)
            }

            else -> {
                println("Invalid field")
                return
            }
        }

        println("The task is changed")
    }

    private fun askUserForFieldToEditName(): String {
        while (true) {
            println("Input a field to edit (priority, date, time, task):")

            val fieldName = readln().lowercase()

            if (fieldName !in listOf("priority", "date", "time", "task")) {
                println("Invalid field")
                continue
            }

            return fieldName
        }
    }

    private fun startTasksDeletion() {
        val tasksAmount = tasker.getTasksAmount()

        if (tasksAmount <= 0) {
            println("No tasks have been input")
            return
        }

        printFormattedTasks()

        val taskNumber = askForUserTaskNumber(tasksAmount)

        tasker.deleteTask(taskNumber)
        println("The task is deleted")
    }

    private fun askForUserTaskNumber(tasksAmount: Int): Int {
        while (true) {
            println("Input the task number (1-$tasksAmount):")

            val taskNumber = readln().toIntOrNull()

            if (taskNumber == null || taskNumber !in 1..tasksAmount) {
                println("Invalid task number")
                continue
            }

            return taskNumber
        }
    }

    private fun printFormattedTasks() {
        println(tasker.tasksTable())
    }
}