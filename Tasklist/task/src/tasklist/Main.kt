package tasklist

import tasklist.commands.Asker
import tasklist.tasks.Tasker

fun main() {
    val tasker = Tasker()
    val asker = Asker(tasker)

    asker.initDialogue()
}


