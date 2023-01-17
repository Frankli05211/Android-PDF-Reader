package net.codebot.pdfviewer

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class saveData(
    pencilP: ArrayList<CustomPath?>,
    brushP: ArrayList<CustomPath?>,
    undoSP: Stack<CustomPath>,
    redoSP: Stack<CustomPath>,
    undoS: Memento,
    redoS: Memento,
    imageOrientation: String): Serializable {
    var pencilPaths: ArrayList<CustomPath?>
    var brushPaths: ArrayList<CustomPath?>
    var undoStackPaths: Stack<CustomPath>
    var redoStackPaths: Stack<CustomPath>
    var undoStack: Memento
    var redoStack: Memento
    var orientation: String

    init {
        pencilPaths = pencilP
        brushPaths = brushP
        undoStackPaths = undoSP
        redoStackPaths = redoSP
        undoStack = undoS
        redoStack = redoS
        orientation = imageOrientation
    }
}
