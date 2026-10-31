package com.notewise.clone.core.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.notewise.clone.core.template.PageTemplate
import com.notewise.clone.core.template.TemplateRenderer
import kotlin.math.hypot

class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Document state
    val strokes = mutableListOf<DrawingPath>()
    val undoRedoManager = UndoRedoManager()
    var currentTool: DrawingTool = DrawingTool.Pen()
    var currentTemplate: PageTemplate = PageTemplate.Ruled()
    var pdfBackgroundBitmap: Bitmap? = null

    // Callbacks
    var onStrokeChangedListener: (() -> Unit)? = null
    var onUndoRedoStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null
    var onTextToolTapped: ((x: Float, y: Float) -> Unit)? = null
    var onStrokeTapped: ((DrawingPath) -> Unit)? = null
    var currentAudioTimestampMsProvider: (() -> Long?)? = null

    // Transform matrix for panning & zooming
    val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()
    var scaleFactor = 1.0f
        private set

    // Palm rejection & Stylus
    var isStylusOnlyMode = false
    private var isStylusActive = false

    // Active in-progress stroke
    private var activePath: DrawingPath? = null
    private var shapeStartPoint: DrawingPoint? = null

    // Lasso selection state
    private val lassoPoints = mutableListOf<DrawingPoint>()
    val selectedStrokes = mutableListOf<DrawingPath>()
    val selectionBounds = RectF()
    private var isDraggingSelection = false
    private var lastDragX = 0f
    private var lastDragY = 0f

    // Ruler tool guide
    var isRulerEnabled: Boolean = false
    var rulerY: Float = 400f

    // Paints
    private val penPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val highlighterPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.BEVEL
        strokeCap = Paint.Cap.SQUARE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    private val activeStrokePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val lassoOverlayPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#3B82F6")
        strokeWidth = 2.5f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    private val selectionBoxPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#2563EB")
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(16f, 8f), 0f)
    }

    private val selectionFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.argb(25, 37, 99, 235)
    }

    private val rulerPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.argb(120, 59, 130, 246)
        strokeWidth = 4f
    }

    private val templateRenderer = TemplateRenderer()

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = detector.scaleFactor
                val targetScale = (scaleFactor * scale).coerceIn(0.4f, 5.0f)
                val effectiveScale = targetScale / scaleFactor
                scaleFactor = targetScale

                drawMatrix.postScale(effectiveScale, effectiveScale, detector.focusX, detector.focusY)
                invalidate()
                return true
            }
        }
    )

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun loadPageData(newStrokes: List<DrawingPath>, template: PageTemplate, pdfBitmap: Bitmap? = null) {
        strokes.clear()
        strokes.addAll(newStrokes)
        currentTemplate = template
        pdfBackgroundBitmap = pdfBitmap
        undoRedoManager.clear()
        selectedStrokes.clear()
        activePath = null
        notifyUndoRedoState()
        invalidate()
    }

    fun resetZoom() {
        drawMatrix.reset()
        scaleFactor = 1.0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(drawMatrix)

        // 1. Draw Page Background & Template
        canvas.drawColor(currentTemplate.backgroundColor)
        pdfBackgroundBitmap?.let { bmp ->
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }
        templateRenderer.render(canvas, width.toFloat(), height.toFloat(), currentTemplate)

        // 2. Draw Ruler Guide if enabled
        if (isRulerEnabled) {
            canvas.drawLine(0f, rulerY, width.toFloat(), rulerY, rulerPaint)
        }

        // 3. Draw Highlighters (bottom layer)
        for (stroke in strokes) {
            if (stroke.tool is DrawingTool.Highlighter) {
                highlighterPaint.color = stroke.color
                highlighterPaint.strokeWidth = stroke.strokeWidth
                canvas.drawPath(stroke.getPath(), highlighterPaint)
            }
        }

        // 4. Draw Inks, Shapes, Text (main layer)
        for (stroke in strokes) {
            when (val tool = stroke.tool) {
                is DrawingTool.Pen -> {
                    penPaint.color = stroke.color
                    penPaint.strokeWidth = stroke.strokeWidth
                    penPaint.alpha = (tool.opacity * 255).toInt().coerceIn(0, 255)
                    canvas.drawPath(stroke.getPath(), penPaint)
                }
                is DrawingTool.Shape -> {
                    penPaint.color = stroke.color
                    penPaint.strokeWidth = stroke.strokeWidth
                    penPaint.style = if (tool.isFilled) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                    canvas.drawPath(stroke.getPath(), penPaint)
                    penPaint.style = Paint.Style.STROKE
                }
                is DrawingTool.Text -> {
                    textPaint.color = stroke.color
                    textPaint.textSize = tool.fontSize
                    if (stroke.points.isNotEmpty()) {
                        canvas.drawText(tool.text, stroke.points[0].x, stroke.points[0].y, textPaint)
                    }
                }
                else -> {}
            }
        }

        // 5. Draw In-Progress Active Stroke (Zero-latency visual feedback)
        activePath?.let { stroke ->
            when (stroke.tool) {
                is DrawingTool.Highlighter -> {
                    activeStrokePaint.color = stroke.color
                    activeStrokePaint.strokeWidth = stroke.strokeWidth
                    activeStrokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                }
                else -> {
                    activeStrokePaint.color = stroke.color
                    activeStrokePaint.strokeWidth = stroke.strokeWidth
                    activeStrokePaint.xfermode = null
                }
            }
            canvas.drawPath(stroke.getPath(), activeStrokePaint)
        }

        // 6. Draw In-Progress Lasso Loop
        if (currentTool is DrawingTool.Lasso && lassoPoints.size > 1) {
            val lassoPath = Path()
            lassoPath.moveTo(lassoPoints[0].x, lassoPoints[0].y)
            for (i in 1 until lassoPoints.size) {
                lassoPath.lineTo(lassoPoints[i].x, lassoPoints[i].y)
            }
            canvas.drawPath(lassoPath, lassoOverlayPaint)
        }

        // 7. Draw Selection Box around selected strokes
        if (selectedStrokes.isNotEmpty()) {
            canvas.drawRect(selectionBounds, selectionFillPaint)
            canvas.drawRect(selectionBounds, selectionBoxPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Multi-touch gestures (Pinch-to-zoom / Pan with 2 fingers)
        if (event.pointerCount > 1) {
            if (activePath != null) {
                activePath = null
                invalidate()
            }
            scaleGestureDetector.onTouchEvent(event)
            return true
        }

        val toolType = event.getToolType(0)
        if (isStylusOnlyMode && toolType != MotionEvent.TOOL_TYPE_STYLUS) {
            // Reject finger touch when stylus mode is active
            return false
        }
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            isStylusActive = true
        }

        // Map touch coordinates to canvas document space
        drawMatrix.invert(inverseMatrix)
        val pts = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(pts)
        val docX = pts[0]
        val docY = pts[1]
        val pressure = if (toolType == MotionEvent.TOOL_TYPE_STYLUS) event.pressure else 1.0f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(docX, docY, pressure)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(docX, docY, pressure, event)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleTouchUp(docX, docY, pressure)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(x: Float, y: Float, pressure: Float) {
        lastDragX = x
        lastDragY = y

        // Check if user tapped inside existing selection box to drag it
        if (selectedStrokes.isNotEmpty() && selectionBounds.contains(x, y)) {
            isDraggingSelection = true
            return
        } else if (selectedStrokes.isNotEmpty()) {
            // Tapped outside selection -> deselect
            selectedStrokes.clear()
            invalidate()
        }

        when (val tool = currentTool) {
            is DrawingTool.Pen -> {
                val width = if (tool.pressureSensitive) {
                    when (tool.penType) {
                        PenType.FOUNTAIN -> tool.width * (0.4f + pressure * 1.0f)
                        PenType.BALLPOINT -> tool.width
                        PenType.CALLIGRAPHY -> tool.width * (0.7f + pressure * 0.6f)
                    }
                } else tool.width

                val path = DrawingPath(
                    tool = tool,
                    color = tool.color,
                    strokeWidth = width,
                    audioTimestampMs = currentAudioTimestampMsProvider?.invoke()
                )
                path.points.add(DrawingPoint(x, y, pressure))
                activePath = path
                invalidate()
            }
            is DrawingTool.Highlighter -> {
                val path = DrawingPath(
                    tool = tool,
                    color = tool.color,
                    strokeWidth = tool.width,
                    audioTimestampMs = currentAudioTimestampMsProvider?.invoke()
                )
                path.points.add(DrawingPoint(x, y, pressure))
                activePath = path
                invalidate()
            }
            is DrawingTool.Shape -> {
                shapeStartPoint = DrawingPoint(x, y, pressure)
                val path = DrawingPath(
                    tool = tool,
                    color = tool.color,
                    strokeWidth = tool.width,
                    audioTimestampMs = currentAudioTimestampMsProvider?.invoke()
                )
                path.points.add(shapeStartPoint!!)
                path.points.add(shapeStartPoint!!)
                activePath = path
                invalidate()
            }
            is DrawingTool.Eraser -> {
                eraseAt(x, y, tool.radius, tool.isStrokeEraser)
            }
            is DrawingTool.Lasso -> {
                lassoPoints.clear()
                lassoPoints.add(DrawingPoint(x, y, pressure))
                invalidate()
            }
            is DrawingTool.Text -> {
                onTextToolTapped?.invoke(x, y)
            }
        }
    }

    private fun handleTouchMove(x: Float, y: Float, pressure: Float, event: MotionEvent) {
        if (isDraggingSelection && selectedStrokes.isNotEmpty()) {
            val dx = x - lastDragX
            val dy = y - lastDragY
            for (stroke in selectedStrokes) {
                stroke.translate(dx, dy)
            }
            selectionBounds.offset(dx, dy)
            lastDragX = x
            lastDragY = y
            invalidate()
            return
        }

        when (currentTool) {
            is DrawingTool.Pen, is DrawingTool.Highlighter -> {
                val historySize = event.historySize
                for (h in 0 until historySize) {
                    val hPts = floatArrayOf(event.getHistoricalX(h), event.getHistoricalY(h))
                    inverseMatrix.mapPoints(hPts)
                    val hPressure = event.getHistoricalPressure(h)
                    addPointToActiveStroke(hPts[0], hPts[1], hPressure)
                }
                addPointToActiveStroke(x, y, pressure)
                invalidate()
            }
            is DrawingTool.Shape -> {
                activePath?.let { stroke ->
                    if (stroke.points.size >= 2) {
                        stroke.points[1] = DrawingPoint(x, y, pressure)
                        stroke.invalidateCache()
                        invalidate()
                    }
                }
            }
            is DrawingTool.Lasso -> {
                lassoPoints.add(DrawingPoint(x, y, pressure))
                invalidate()
            }
            is DrawingTool.Eraser -> {
                val eraser = currentTool as DrawingTool.Eraser
                eraseAt(x, y, eraser.radius, eraser.isStrokeEraser)
            }
            else -> {}
        }
    }

    private fun addPointToActiveStroke(x: Float, y: Float, pressure: Float) {
        activePath?.let { stroke ->
            stroke.points.add(DrawingPoint(x, y, pressure))
            stroke.invalidateCache()
        }
    }

    private fun handleTouchUp(x: Float, y: Float, pressure: Float) {
        if (isDraggingSelection) {
            isDraggingSelection = false
            onStrokeChangedListener?.invoke()
            return
        }

        when (currentTool) {
            is DrawingTool.Pen, is DrawingTool.Highlighter, is DrawingTool.Shape -> {
                activePath?.let { stroke ->
                    if (currentTool !is DrawingTool.Shape) {
                        stroke.points.add(DrawingPoint(x, y, pressure))
                    }
                    stroke.invalidateCache()
                    strokes.add(stroke)
                    undoRedoManager.recordAction(UndoRedoManager.CanvasAction.AddStroke(stroke))
                    notifyUndoRedoState()
                    onStrokeChangedListener?.invoke()
                    activePath = null
                    shapeStartPoint = null
                    invalidate()
                }
            }
            is DrawingTool.Lasso -> {
                if (lassoPoints.size > 2) {
                    finishLassoSelection()
                } else {
                    // Tap detection for stroke playback seek
                    val tapped = strokes.find { it.intersects(x, y, 20f) }
                    tapped?.let { onStrokeTapped?.invoke(it) }
                }
                lassoPoints.clear()
                invalidate()
            }
            else -> {}
        }
    }

    private fun finishLassoSelection() {
        selectedStrokes.clear()
        for (stroke in strokes) {
            if (stroke.isInsidePolygon(lassoPoints)) {
                selectedStrokes.add(stroke)
            }
        }

        if (selectedStrokes.isNotEmpty()) {
            // Compute combined selection bounding box
            selectionBounds.set(selectedStrokes[0].bounds)
            for (i in 1 until selectedStrokes.size) {
                selectionBounds.union(selectedStrokes[i].bounds)
            }
            selectionBounds.inset(-16f, -16f)
        }
    }

    fun deleteSelectedStrokes() {
        if (selectedStrokes.isEmpty()) return
        val removed = mutableListOf<Pair<DrawingPath, Int>>()
        for (stroke in selectedStrokes) {
            val idx = strokes.indexOf(stroke)
            if (idx != -1) {
                removed.add(stroke to idx)
                strokes.removeAt(idx)
            }
        }
        selectedStrokes.clear()
        if (removed.isNotEmpty()) {
            undoRedoManager.recordAction(UndoRedoManager.CanvasAction.BatchRemove(removed))
            notifyUndoRedoState()
            onStrokeChangedListener?.invoke()
            invalidate()
        }
    }

    fun recolorSelectedStrokes(newColor: Int) {
        for (stroke in selectedStrokes) {
            stroke.color = newColor
            stroke.invalidateCache()
        }
        onStrokeChangedListener?.invoke()
        invalidate()
    }

    fun duplicateSelectedStrokes() {
        if (selectedStrokes.isEmpty()) return
        val duplicated = mutableListOf<DrawingPath>()
        for (stroke in selectedStrokes) {
            val clonePoints = stroke.points.map { it.copy(x = it.x + 40f, y = it.y + 40f) }.toMutableList()
            val clone = DrawingPath(
                points = clonePoints,
                tool = stroke.tool,
                color = stroke.color,
                strokeWidth = stroke.strokeWidth
            )
            strokes.add(clone)
            duplicated.add(clone)
            undoRedoManager.recordAction(UndoRedoManager.CanvasAction.AddStroke(clone))
        }
        selectedStrokes.clear()
        selectedStrokes.addAll(duplicated)
        // Update selection bounds
        selectionBounds.set(selectedStrokes[0].bounds)
        for (i in 1 until selectedStrokes.size) {
            selectionBounds.union(selectedStrokes[i].bounds)
        }
        selectionBounds.inset(-16f, -16f)
        notifyUndoRedoState()
        onStrokeChangedListener?.invoke()
        invalidate()
    }

    private fun eraseAt(x: Float, y: Float, radius: Float, isStrokeEraser: Boolean) {
        val removed = mutableListOf<Pair<DrawingPath, Int>>()
        val iterator = strokes.iterator()
        var index = 0

        while (iterator.hasNext()) {
            val stroke = iterator.next()
            if (stroke.intersects(x, y, radius)) {
                removed.add(stroke to index)
                iterator.remove()
            }
            index++
        }

        if (removed.isNotEmpty()) {
            if (removed.size == 1) {
                undoRedoManager.recordAction(UndoRedoManager.CanvasAction.RemoveStroke(removed[0].first, removed[0].second))
            } else {
                undoRedoManager.recordAction(UndoRedoManager.CanvasAction.BatchRemove(removed))
            }
            notifyUndoRedoState()
            onStrokeChangedListener?.invoke()
            invalidate()
        }
    }

    fun addTextAnnotation(text: String, x: Float, y: Float, fontSize: Float = 24f, color: Int = Color.BLACK) {
        val stroke = DrawingPath(
            tool = DrawingTool.Text(text = text, fontSize = fontSize, color = color),
            color = color,
            strokeWidth = 2f,
            audioTimestampMs = currentAudioTimestampMsProvider?.invoke()
        )
        stroke.points.add(DrawingPoint(x, y))
        strokes.add(stroke)
        undoRedoManager.recordAction(UndoRedoManager.CanvasAction.AddStroke(stroke))
        notifyUndoRedoState()
        onStrokeChangedListener?.invoke()
        invalidate()
    }

    fun undo() {
        if (undoRedoManager.undo(strokes)) {
            notifyUndoRedoState()
            onStrokeChangedListener?.invoke()
            invalidate()
        }
    }

    fun redo() {
        if (undoRedoManager.redo(strokes)) {
            notifyUndoRedoState()
            onStrokeChangedListener?.invoke()
            invalidate()
        }
    }

    fun clearCanvas() {
        if (strokes.isEmpty()) return
        val allStrokes = strokes.mapIndexed { idx, s -> s to idx }
        strokes.clear()
        undoRedoManager.recordAction(UndoRedoManager.CanvasAction.BatchRemove(allStrokes))
        notifyUndoRedoState()
        onStrokeChangedListener?.invoke()
        invalidate()
    }

    private fun notifyUndoRedoState() {
        onUndoRedoStateChanged?.invoke(undoRedoManager.canUndo(), undoRedoManager.canRedo())
    }
}
