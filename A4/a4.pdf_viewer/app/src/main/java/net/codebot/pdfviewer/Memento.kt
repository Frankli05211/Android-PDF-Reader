package net.codebot.pdfviewer

import android.util.Log
import java.io.Serializable
import java.util.Stack

class Memento: Stack<Pair<String, Pair<String, CustomPath?>>>(), Serializable{
    var paths: Stack<CustomPath> = Stack()

    override fun push(item: Pair<String, Pair<String, CustomPath?>>): Pair<String, Pair<String, CustomPath?>> {
        // Supply at most 5 undo/redo
        if (this.size == 5) {
            paths.removeAt(0)
            this.removeAt(0)
        }

        // Push data
        paths.push(item.second.second)

        return super.push(item)
    }

    override fun pop(): Pair<String, Pair<String, CustomPath?>> {
        // Pop data
        paths.pop()

        return super.pop()
    }

    override fun removeAllElements() {
        // remove data
        paths.removeAllElements()

        super.removeAllElements()
    }

    // The function will draw the paths stored inside the Memento class
    fun drawPaths() {
        if (paths.isNotEmpty()) {
            var i = this.size - 1
            val newStack = Memento()

            while (i >= 0) {
                val item = this[i]
                newStack.push(Pair(item.first, Pair(item.second.first, paths[i])))
                i--
            }

            super.removeAllElements()
            while (newStack.isNotEmpty()) {
                val item = newStack.pop()
                super.push(item)
            }
        }
    }
}
