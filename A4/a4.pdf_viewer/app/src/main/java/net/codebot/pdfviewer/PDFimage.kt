package net.codebot.pdfviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.graphics.PathUtils
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("AppCompatCustomView")
class PDFimage  // constructor
    (context: Context?) : ImageView(context) {
    val LOGNAME = "pdf_image"

    // drawing path
    var path: CustomPath? = null
    var pencilPaths = ArrayList<CustomPath?>()
    var brushPaths = ArrayList<CustomPath?>()

    // image to display
    var bitmap: Bitmap? = null
    var pencilPaint = Paint()
    var brushPaint = Paint()

    // Control the eraser feature
    var eraser: Boolean = false

    // The reference of the current image
    val currentImage = this

    // The undo and redo stack
    var undoStack = Memento()
    var redoStack = Memento()

    // Control system features
    enum class DRAW {PENCIL, BRUSH, NONE}
    enum class MODE {DRAW, ZOOM, DRAG, ERASER, NONE}
    var drawMethod = DRAW.NONE
    var imageMode = MODE.NONE

    // Generate global variable for actions
    private var startX: Float = 0.0F
    private var startY: Float = 0.0F
    private var midX: Float = 0.0F
    private var midY: Float = 0.0F
    private var oldDist: Float = 1.0F
    private var zoomPointer1: Int = 0
    private var zoomPointer2: Int = 0

    init {
        pencilPaint.style = Paint.Style.STROKE
        pencilPaint.strokeWidth = 5f
        pencilPaint.color = Color.rgb(0, 0, 100)

        brushPaint.style = Paint.Style.STROKE
        brushPaint.strokeWidth = 30f
        brushPaint.color = Color.YELLOW
        brushPaint.alpha = 100
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                when (imageMode) {
                    MODE.DRAW -> {
                        imageMode = MODE.DRAW
                        path = CustomPath()
                        path!!.moveTo(event.x, event.y)
                    }
                    MODE.ERASER -> {
                        var iter = pencilPaths.iterator()
                        while (iter.hasNext()) {
                            val currentPath = iter.next()
                            val pathSegment = PathUtils.flatten(currentPath!!)
                            val currentPoint = PointF(event.x, event.y)
                            var isRemoved = false
                            for (segment in pathSegment) {
                                if (closestPoint(currentPoint, segment.start, segment.end) <= 20) {
                                    isRemoved = true
                                    break
                                }
                            }

                            if (isRemoved) {
                                undoStack.push(Pair("Remove", Pair("Pencil", currentPath)))
                                redoStack.removeAllElements()
                                iter.remove()
                            }
                        }

                        iter = brushPaths.iterator()
                        while (iter.hasNext()) {
                            val currentPath = iter.next()
                            val pathSegment = PathUtils.flatten(currentPath!!)
                            val currentPoint = PointF(event.x, event.y)
                            var isRemoved = false
                            for (segment in pathSegment) {
                                if (closestPoint(currentPoint, segment.start, segment.end) <= 45) {
                                    isRemoved = true
                                    break
                                }
                            }

                            if (isRemoved) {
                                undoStack.push(Pair("Remove", Pair("Brush", currentPath)))
                                redoStack.removeAllElements()
                                iter.remove()
                            }
                        }
                    }
                    else -> {
                        imageMode = MODE.DRAG
                        startX = event.x
                        startY = event.y
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (imageMode != MODE.DRAW && imageMode != MODE.ERASER) {
                    // Find the distance from touched two points
                    val firstPoint = PointF(event.getX(0), event.getY(0))
                    val secondPoint = PointF(event.getX(1), event.getY(1))
                    oldDist = findPointDistance(firstPoint, secondPoint)
                    zoomPointer1 = event.getPointerId(0)
                    zoomPointer2 = event.getPointerId(1)
                    if (oldDist > 10F) {
                        imageMode = MODE.ZOOM
                        val mid = findMid(event.getX(0), event.getY(0),
                            event.getX(1), event.getY(1))
                        midX = mid.x
                        midY = mid.y
                        Log.d(LOGNAME, "The midpoint is (${midX}, ${midY})")
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (imageMode) {
                    MODE.DRAW -> {
                        when (drawMethod) {
                            DRAW.PENCIL -> {
                                path!!.lineTo(event.x, event.y)
                                pencilPaths.remove(path)
                                pencilPaths.add(path)
                            }
                            DRAW.BRUSH -> {
                                path!!.lineTo(event.x, event.y)
                                brushPaths.remove(path)
                                brushPaths.add(path)
                            }
                            DRAW.NONE -> {}
                        }
                    }
                    MODE.ZOOM -> {
                        // Get two current pointer's position
                        val (x1: Float, y1: Float) = event.findPointerIndex(zoomPointer1).let { pointerIndex ->
                            // Get the pointer's current position
                            event.getX(pointerIndex) to event.getY(pointerIndex)
                        }


                        val (x2: Float, y2: Float) = event.findPointerIndex(zoomPointer2).let { pointerIndex ->
                            // Get the pointer's current position
                            event.getX(pointerIndex) to event.getY(pointerIndex)
                        }

                        // Find the distance from touched two points
                        val firstPoint = PointF(x1, y1)
                        val secondPoint = PointF(x2, y2)

                        val dist = findPointDistance(firstPoint, secondPoint)
                        val newDistX = dist * currentImage.scaleX
                        val newDistY = dist * currentImage.scaleY
                        if (newDistX > 10F && newDistY > 10F) {
                            // Find the translate of image

//                            val tX = (midPoint.x - midX) * currentImage.scaleX
//                            val tY = (midPoint.y - midY) * currentImage.scaleY
//                            val translateX = midPoint.x - midX
//                            val translateY = midPoint.y - midY
//                            midX = midPoint.x
//                            midY = midPoint.y
//                            Log.d(LOGNAME, "The midpoint is (${tX}, ${tY})")

                            var scaleX = newDistX / oldDist
                            var scaleY = newDistY / oldDist
                            scaleX = max(0.5F, min(scaleX, 20.0F))
                            scaleY = max(0.5F, min(scaleY, 20.0F))
//                            val translateX = midX * scaleX - currentImage.x * scaleX
//                            val translateY = midY * scaleY - currentImage.y * scaleY
//                            currentImage.translationX = midX * scaleX
//                            currentImage.translationY = midY * scaleY
                            currentImage.scaleX = scaleX
                            currentImage.scaleY = scaleY
//                            currentImage.x -= midX * scaleX
//                            currentImage.y -= midY * scaleY
//                            val midPoint = findMid(firstPoint.x, firstPoint.y,
//                                secondPoint.x, secondPoint.y)
//                            currentImage.x += (900.0F - midX) * scaleX
//                            currentImage.y += (980.0F - midY) * scaleY
//                            currentImage.x = translateX
//                            currentImage.y = translateY
//                            Log.d(LOGNAME, "The translateX is ${midPoint.x}")
//                            Log.d(LOGNAME, "The translateY is ${midPoint.y}")
//                            val matrix = Matrix()
//                            matrix.reset()
//                            matrix.postScale(scaleX, scaleY, midX, midY)
//                            transformMatrix.set()
//                            currentImage.imageMatrix = Matrix().apply {
//                                postScale(scaleX, scaleY, midX, midY)
//                            }
//                            Log.d(LOGNAME, "The maxtrix is ${matrix}")
//                            Log.d(LOGNAME, "The translateX is ${translateX}")
//                            Log.d(LOGNAME, "The translateY is ${translateY}")
//                            val moveX = currentImage.translationX + tX
//                            val moveY = currentImage.translationY + tY
//                            currentImage.x = translateX
//                            currentImage.y = translateY
                        }
                    }
                    MODE.DRAG -> {
                        // Retrieve the current cursor position
                        val currPoint = PointF(event.getX(0), event.getY(0))
                        val moveX = currentImage.translationX + (currPoint.x - startX) * currentImage.scaleX
                        val moveY = currentImage.translationY + (currPoint.y - startY) * currentImage.scaleY
                        currentImage.translationX = moveX
                        currentImage.translationY = moveY
                    }
                    else -> {}
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (imageMode == MODE.ZOOM) {
                    imageMode = MODE.NONE
                }
            }
            MotionEvent.ACTION_UP -> {
                when (imageMode) {
                    MODE.DRAW -> {
                        when (drawMethod) {
                            DRAW.PENCIL -> {
                                pencilPaths.remove(path)
                                pencilPaths.add(path)
                                undoStack.push(Pair("Add", Pair("Pencil", path)))
                                redoStack.removeAllElements()
                            }
                            DRAW.BRUSH -> {
                                brushPaths.remove(path)
                                brushPaths.add(path)
                                undoStack.push(Pair("Add", Pair("Brush", path)))
                                redoStack.removeAllElements()
                            }
                            DRAW.NONE -> {}
                        }
                    }
                    MODE.DRAG -> {
                        imageMode = MODE.NONE
                    }
                    else -> {}
                }
            }
        }
        return true
    }

    // set image as background
    fun setImage(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDraw(canvas: Canvas) {
        // draw background
        if (bitmap != null) {
            setImageBitmap(bitmap)
        }

        // draw lines using brush
        for (path in brushPaths) {
            canvas.drawPath(path!!, brushPaint)
        }

        // draw lines using pencil
        for (path in pencilPaths) {
            val pathSegment = PathUtils.flatten(path!!)

            for (segment in pathSegment) {

            }
            canvas.drawPath(path!!, pencilPaint)
        }

        super.onDraw(canvas)
    }

    // The function calculate the distance between two points
    private fun findPointDistance(A: PointF, B:PointF): Float {
        val squareX = (A.x.toDouble() - B.x.toDouble()).pow(2.0)
        val squareY = (A.y.toDouble() - B.y.toDouble()).pow(2.0)
        val squareSum = squareX.toFloat() + squareY.toFloat()
        return sqrt(squareSum)
    }

    // The function implement the dot product of two point
    private fun dotProduct(A: PointF, B: PointF): Float {
        return  A.x * B.x + A.y * B.y
    }

    // Find the distance between M and its closest point at line starting at P0 and ending at P1
    private fun closestPoint(M: PointF, P0: PointF, P1: PointF): Float {
        val v = P1.minus(P0) // v = P1 - P0
        // early out if line is less than 1 pixel long
        if (findPointDistance(P0, P1) < 1.0)
            return findPointDistance(P0, M)

        val u = M.minus(P0) // u = M - P0

        // scalar of vector projection ...
        val s = dotProduct(u, v) / dotProduct(v, v)

        // find point for constrained line segment
        return if (s < 0)
            findPointDistance(P0, M)
        else if (s > 1)
            findPointDistance(P1, M)
        else {
            val w = PointF(v.x * s, v.y * s)
            findPointDistance(P0.plus(w), M)
        }
    }

    // The function will determine whether the input two paths are the same
    @RequiresApi(Build.VERSION_CODES.O)
    fun comparePaths(path1:CustomPath, path2:CustomPath): Boolean {
        val pathSegment1 = PathUtils.flatten(path1)
        val pathSegment2 = PathUtils.flatten(path2)
        if (pathSegment1.size != pathSegment2.size) {
            return false
        } else {
            val iter1 = pathSegment1.iterator()
            val iter2 = pathSegment2.iterator()
            while (iter1.hasNext() && iter2.hasNext()) {
                val point1 = iter1.next()
                val point2 = iter2.next()
                if (point1.start != point2.start || point1.end != point2.end) {
                    return false
                }
            }
        }

        return true
    }

    // The function will undo an action stored in the stack
    @RequiresApi(Build.VERSION_CODES.O)
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val undoElement = undoStack.pop()

            when (undoElement.first) {
                "Add" -> {
                    val currentPath = undoElement.second.second
                    if (undoElement.second.first == "Pencil") {
                        redoStack.push(Pair("Remove", Pair("Pencil", currentPath)))

                        if (pencilPaths.indexOf(currentPath) == -1) {
                            for (paths in pencilPaths) {
                                if (comparePaths(paths!!, currentPath!!)) {
                                    pencilPaths.remove(paths)
                                    break
                                }
                            }
                        } else {
                            pencilPaths.remove(currentPath)
                        }
                    } else {
                        redoStack.push(Pair("Remove", Pair("Brush", currentPath)))

                        if (brushPaths.indexOf(currentPath) == -1 ) {
                            for (paths in brushPaths) {
                                if (comparePaths(paths!!, currentPath!!)) {
                                    brushPaths.remove(paths)
                                    break
                                }
                            }
                        } else {
                            brushPaths.remove(currentPath)
                        }
                    }
                }
                "Remove" -> {
                    val currentPath = undoElement.second.second
                    if (undoElement.second.first == "Pencil") {
                        redoStack.push(Pair("Add", Pair("Pencil", currentPath)))
                        pencilPaths.add(currentPath)
                    } else {
                        redoStack.push(Pair("Add", Pair("Brush", currentPath)))
                        brushPaths.add(currentPath)
                    }
                }
            }
        }
    }

    // The function will redo an action stored in the stack
    @RequiresApi(Build.VERSION_CODES.O)
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val redoElement = redoStack.pop()

            when(redoElement.first) {
                "Add" -> {
                    val currentPath = redoElement.second.second
                    if (redoElement.second.first == "Pencil") {
                        undoStack.push(Pair("Remove", Pair("Pencil", currentPath)))

                        if (pencilPaths.indexOf(currentPath) == -1 ) {
                            for (paths in pencilPaths) {
                                if (comparePaths(paths!!, currentPath!!)) {
                                    pencilPaths.remove(paths)
                                    break
                                }
                            }
                        } else {
                            pencilPaths.remove(currentPath)
                        }
                    } else {
                        undoStack.push(Pair("Remove", Pair("Brush", currentPath)))

                        if (brushPaths.indexOf(currentPath) == -1 ) {
                            for (paths in brushPaths) {
                                if (comparePaths(paths!!, currentPath!!)) {
                                    brushPaths.remove(paths)
                                    break
                                }
                            }
                        } else {
                            brushPaths.remove(currentPath)
                        }
                    }
                }
                "Remove" -> {
                    val currentPath = redoElement.second.second
                    if (redoElement.second.first == "Pencil") {
                        undoStack.push(Pair("Add", Pair("Pencil", currentPath)))
                        pencilPaths.add(currentPath)
                    } else {
                        undoStack.push(Pair("Add", Pair("Brush", currentPath)))
                        brushPaths.add(currentPath)
                    }
                }
            }
        }
    }

    // The function will return the midpoint of the given two points
    fun findMid(firstX: Float, firstY: Float, secondX: Float, secondY: Float): PointF {
        val midPointX = (firstX + secondX) / 2
        val midPointY = (firstY + secondY) / 2
        return PointF(midPointX, midPointY)
    }
}