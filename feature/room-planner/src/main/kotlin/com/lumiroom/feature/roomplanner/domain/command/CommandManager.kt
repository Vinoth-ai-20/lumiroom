package com.lumiroom.feature.roomplanner.domain.command

class CommandManager {
    private val undoStack = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun execute(command: Command) {
        command.execute()
        undoStack.add(command)
        redoStack.clear()
        
        // Optional: limit stack size to prevent OOM
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (canUndo) {
            val command = undoStack.removeLast()
            command.undo()
            redoStack.add(command)
        }
    }

    fun redo() {
        if (canRedo) {
            val command = redoStack.removeLast()
            command.execute()
            undoStack.add(command)
        }
    }
}
