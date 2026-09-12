package com.kalidroid.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lines = mutableListOf(AnsiLine("KaliDroid terminal ready."))
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DFF7F1")
        textSize = 34f
        typeface = Typeface.MONOSPACE
    }
    private val bgPaint = Paint().apply { color = Color.parseColor("#02080D") }

    fun append(raw: String) {
        raw.split('\n').forEach { line ->
            lines += AnsiLine(stripAnsi(line))
        }
        while (lines.size > 200) lines.removeAt(0)
        requestLayout()
        invalidate()
    }

    fun clear() {
        lines.clear()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (lines.size * lineHeight() + paddingTop + paddingBottom + 24).toInt().coerceAtLeast(200)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        var y = paddingTop + paint.textSize
        lines.forEach { line ->
            paint.color = line.color
            canvas.drawText(line.text, paddingLeft.toFloat() + 16f, y, paint)
            y += lineHeight()
        }
    }

    private fun lineHeight(): Float = paint.textSize + 14f

    private fun stripAnsi(input: String): String {
        return ANSI_REGEX.replace(input, "")
    }

    data class AnsiLine(
        val text: String,
        val color: Int = Color.parseColor("#3DE0C5")
    )

    companion object {
        private val ANSI_REGEX = Regex("\\u001B\\[[0-9;]*[A-Za-z]")
    }
}
