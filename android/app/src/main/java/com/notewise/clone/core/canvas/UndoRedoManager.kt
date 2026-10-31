package com.notewise.clone.core.canvas

class UndoRedoManager(private val maxHistory: Int = 50) {
    private val undoStack = ArrayDeque<CanvasAction>()
    private val redoStack = ArrayDeque<CanvasAction>()

    sealed class CanvasAction {
        data class AddStroke(val stroke: DrawingPath) : CanvasAction()
        data class RemoveStroke(val stroke: DrawingPath, val index: Int) : CanvasAction()
        data class BatchRemove(val strokesWithIndices: List<Pair<DrawingPath, Int>>) : CanvasAction()
    }

    fun recordAction(action: CanvasAction) {
        if (undoStack.size >= maxHistory) {
            undoStack.removeFirst()
        }
        undoStack.addLast(action)
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(currentStrokes: MutableList<DrawingPath>): Boolean {
        if (undoStack.isEmpty()) return false
        val action = undoStack.removeLast()
        redoStack.addLast(action)

        when (action) {
            is CanvasAction.AddStroke -> {
                currentStrokes.remove(action.stroke)
            }
            is CanvasAction.RemoveStroke -> {
                val idx = action.index.coerceIn(0, currentStrokes.size)
                currentStrokes.add(idx, action.stroke)
            }
            is CanvasAction.BatchRemove -> {
                for ((stroke, idx) in action.strokesWithIndices.reversed()) {
                    val safeIdx = idx.coerceIn(0, currentStrokes.size)
                    currentStrokes.add(safeIdx, stroke)
                }
            }
        }
        return true
    }

    fun redo(currentStrokes: MutableList<DrawingPath>): Boolean {
        if (redoStack.isEmpty()) return false
        val action = redoStack.removeLast()
        undoStack.addLast(action)

        when (action) {
            is CanvasAction.AddStroke -> {
                currentStrokes.add(action.stroke)
            }
            is CanvasAction.RemoveStroke -> {
                currentStrokes.remove(action.stroke)
            }
            is CanvasAction.BatchRemove -> {
                for ((stroke, _) in action.strokesWithIndices) {
                    currentStrokes.remove(stroke)
                }
            }
        }
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

