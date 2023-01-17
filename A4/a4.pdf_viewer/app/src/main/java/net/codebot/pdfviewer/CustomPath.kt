package net.codebot.pdfviewer

import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.PathUtils
import java.io.Serializable

class CustomPath: Path(), Serializable {
    var points: ArrayList<Pair<Float, Float>> = ArrayList()

    override fun lineTo(x: Float, y: Float) {
        super.lineTo(x, y)
        points.add(Pair(x, y))
    }

    override fun moveTo(x: Float, y: Float) {
        super.moveTo(x, y)
        points.add(Pair(x, y))
    }

    fun drawPath() {
        if (points.isNotEmpty()) {
            val firstPoint = points[0]
            super.moveTo(firstPoint.first, firstPoint.second)

            for (point in points) {
                super.lineTo(point.first, point.second)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePoints() {
        val pathSegment = PathUtils.flatten(this)
        val segmentIter = pathSegment.iterator()
        var i = 0
        points.clear()
        while (segmentIter.hasNext()) {
            val currentSegment = segmentIter.next()
            val currentPoint = currentSegment.start
            points.add(Pair(currentPoint.x, currentPoint.y))
            i++

            if (!segmentIter.hasNext()) {
                val finalPoint = currentSegment.end
                points.add(Pair(finalPoint.x, finalPoint.y))
            }
        }
    }
}