package edu.tcu.mlcoronilla.paint

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint()
    private val pathList = mutableListOf<CustomPath>()
    private var path = CustomPath(Color.BLACK, 10 * resources.displayMetrics.density)

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                pathList.add(path)
                path.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                path = CustomPath(path.color, path.width)
            }
        }

        return true
    }

    //sets the path color from selected color from main that takes it from user input
    fun setPathColor(color: Int) {
        path = CustomPath(color,path.width)
    }

    //set the path from selected width from main from user input
    fun setPathWidth(width: Int) {
        val widthInPx = width * resources.displayMetrics.density
        path = CustomPath(path.color, widthInPx)
    }

    //to remove the last path drawn when the undo image view is clicked on
    fun undoPath() {
        if(pathList.isNotEmpty()) {
            pathList.removeLast()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        for(path in pathList) {
            paint.color = path.color
            paint.strokeWidth = path.width
            canvas.drawPath(path, paint)
        }
    }

    private data class CustomPath(val color: Int, val width: Float) : Path(){

    }
}