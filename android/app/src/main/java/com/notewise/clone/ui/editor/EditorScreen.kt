package com.notewise.clone.ui.editor

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.notewise.clone.NoteApplication
import com.notewise.clone.core.audio.AudioRecordManager
import com.notewise.clone.core.canvas.DrawingCanvasView
import com.notewise.clone.core.canvas.DrawingTool
import com.notewise.clone.core.canvas.PenType
import com.notewise.clone.core.canvas.RulerType
import com.notewise.clone.core.canvas.ShapeType
import com.notewise.clone.core.database.PageEntity
import com.notewise.clone.core.database.StickerEntity
import com.notewise.clone.core.database.StrokeConverter
import com.notewise.clone.core.markdown.MilkdownEditorView
import com.notewise.clone.core.pdf.PdfManager
import com.notewise.clone.core.template.PageTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    notebookId: String,
    notebookTitle: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { (context.applicationContext as NoteApplication).database }
    val strokeConverter = remember { StrokeConverter() }
    val pdfManager = remember { PdfManager(context) }
    val audioRecordManager = remember { AudioRecordManager(context) }

    // Pages state from Room DB
    val pages by db.pageDao().getPagesForNotebook(notebookId).collectAsState(initial = emptyList())
    val stickers by db.stickerDao().getAllStickers().collectAsState(initial = emptyList())
    var currentPageIndex by remember { mutableIntStateOf(0) }

    // Canvas view ref
    var canvasRef by remember { mutableStateOf<DrawingCanvasView?>(null) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var hasLassoSelection by remember { mutableStateOf(false) }

    // Current tool settings
    var selectedToolType by remember { mutableStateOf("pen") }
    var previousPenTool by remember { mutableStateOf("pen") }
    var selectedPenType by remember { mutableStateOf(PenType.FOUNTAIN) }
    var selectedColor by remember { mutableIntStateOf(AndroidColor.BLACK) }
    var selectedPenWidth by remember { mutableFloatStateOf(3.5f) }
    var selectedPenOpacity by remember { mutableFloatStateOf(1.0f) }

    var selectedHighlighterWidth by remember { mutableFloatStateOf(24.0f) }
    var selectedHighlighterColor by remember { mutableIntStateOf(AndroidColor.argb(90, 255, 235, 59)) }

    var selectedEraserRadius by remember { mutableFloatStateOf(24.0f) }
    var isStrokeEraser by remember { mutableStateOf(true) }
    var isEraserAutoDeselect by remember { mutableStateOf(true) }

    // Smart Features
    var isAutoShapeEnabled by remember { mutableStateOf(true) }
    var isScribbleToEraseEnabled by remember { mutableStateOf(true) }
    var isMarkdownMode by remember { mutableStateOf(false) }

    var selectedShapeType by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isRulerEnabled by remember { mutableStateOf(false) }
    var selectedRulerType by remember { mutableStateOf(RulerType.STRAIGHT_EDGE) }
    var isStylusOnlyMode by remember { mutableStateOf(false) }

    // Dialogs & sheets
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showPageOverviewSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showTextEntryDialog by remember { mutableStateOf(false) }
    var textEntryCoord by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var textInput by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Audio Sync State
    var isAudioRecording by remember { mutableStateOf(false) }
    var audioRecordingDurationMs by remember { mutableLongStateOf(0L) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioPlaybackProgress by remember { mutableIntStateOf(0) }
    var audioPlaybackTotal by remember { mutableIntStateOf(0) }
    var hasAudioFile by remember { mutableStateOf(false) }

    // Initialize Page 1 in DB if brand new
    LaunchedEffect(pages) {
        if (pages.isEmpty()) {
            val initialPage = PageEntity(
                notebookId = notebookId,
                pageIndex = 0,
                templateType = "Ruled",
                backgroundColor = "#FFFFFF"
            )
            db.pageDao().upsertPage(initialPage)
            db.notebookDao().updatePageCount(notebookId, 1)
        }
    }

    val currentPage = pages.getOrNull(currentPageIndex)
    LaunchedEffect(currentPage?.id) {
        currentPage?.let { page ->
            val strokes = strokeConverter.toStrokesList(page.strokeJson)
            val template = PageTemplate.fromString(page.templateType, page.backgroundColor)
            canvasRef?.loadPageData(strokes, template)

            val audioFile = File(context.getExternalFilesDir(null), "audio_${page.id}.m4a")
            hasAudioFile = audioFile.exists() && audioFile.length() > 0
        }
    }

    LaunchedEffect(isAudioRecording) {
        while (isAudioRecording) {
            audioRecordingDurationMs = audioRecordManager.getRecordingElapsedMs()
            delay(200)
        }
    }

    fun saveCurrentPageStrokes() {
        val page = currentPage ?: return
        val canvas = canvasRef ?: return
        val json = strokeConverter.fromStrokesList(canvas.strokes)
        scope.launch {
            db.pageDao().updateStrokes(page.id, json)
        }
    }

    fun updateCanvasTool() {
        val canvas = canvasRef ?: return
        canvas.isAutoShapeEnabled = isAutoShapeEnabled
        canvas.isScribbleToEraseEnabled = isScribbleToEraseEnabled
        canvas.isEraserAutoDeselect = isEraserAutoDeselect
        canvas.rulerGuide.isEnabled = isRulerEnabled
        canvas.rulerGuide.rulerType = selectedRulerType

        when (selectedToolType) {
            "pen" -> {
                previousPenTool = "pen"
                canvas.currentTool = DrawingTool.Pen(
                    color = selectedColor,
                    width = selectedPenWidth,
                    opacity = selectedPenOpacity,
                    penType = selectedPenType
                )
            }
            "highlighter" -> {
                previousPenTool = "highlighter"
                canvas.currentTool = DrawingTool.Highlighter(
                    color = selectedHighlighterColor,
                    width = selectedHighlighterWidth
                )
            }
            "eraser" -> canvas.currentTool = DrawingTool.Eraser(
                radius = selectedEraserRadius,
                isStrokeEraser = isStrokeEraser,
                autoDeselect = isEraserAutoDeselect
            )
            "lasso" -> canvas.currentTool = DrawingTool.Lasso
            "shape" -> canvas.currentTool = DrawingTool.Shape(
                shapeType = selectedShapeType,
                color = selectedColor,
                width = selectedPenWidth
            )
            "text" -> canvas.currentTool = DrawingTool.Text()
            "eyedropper" -> canvas.currentTool = DrawingTool.EyeDropper
        }
    }

    LaunchedEffect(selectedToolType, selectedPenType, selectedColor, selectedPenWidth, selectedPenOpacity, selectedHighlighterWidth, selectedHighlighterColor, selectedEraserRadius, isStrokeEraser, isEraserAutoDeselect, isAutoShapeEnabled, isScribbleToEraseEnabled, selectedShapeType, isRulerEnabled, selectedRulerType) {
        updateCanvasTool()
    }

    DisposableEffect(Unit) {
        onDispose {
            saveCurrentPageStrokes()
            audioRecordManager.release()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(notebookTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showPageOverviewSheet = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Page ${currentPageIndex + 1} of ${pages.size.coerceAtLeast(1)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            saveCurrentPageStrokes()
                            onBackClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    saveCurrentPageStrokes()
                                    currentPageIndex--
                                }
                            },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page")
                        }

                        IconButton(
                            onClick = {
                                saveCurrentPageStrokes()
                                if (currentPageIndex < pages.size - 1) {
                                    currentPageIndex++
                                } else {
                                    scope.launch {
                                        val newPageIndex = pages.size
                                        val newPage = PageEntity(
                                            notebookId = notebookId,
                                            pageIndex = newPageIndex,
                                            templateType = currentPage?.templateType ?: "Ruled",
                                            backgroundColor = currentPage?.backgroundColor ?: "#FFFFFF"
                                        )
                                        db.pageDao().upsertPage(newPage)
                                        db.notebookDao().updatePageCount(notebookId, pages.size + 1)
                                        currentPageIndex = newPageIndex
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (currentPageIndex < pages.size - 1) Icons.AutoMirrored.Filled.NavigateNext else Icons.Default.Add,
                                contentDescription = "Next or Add Page"
                            )
                        }

                        // Markdown Toggle
                        IconButton(onClick = { isMarkdownMode = !isMarkdownMode }) {
                            Icon(
                                if (isMarkdownMode) Icons.Default.Draw else Icons.Default.Notes,
                                contentDescription = "Toggle Markdown Note",
                                tint = if (isMarkdownMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }

                        // Audio Record Button
                        IconButton(
                            onClick = {
                                val page = currentPage ?: return@IconButton
                                val audioFile = File(context.getExternalFilesDir(null), "audio_${page.id}.m4a")

                                if (isAudioRecording) {
                                    val duration = audioRecordManager.stopRecording()
                                    isAudioRecording = false
                                    hasAudioFile = true
                                    scope.launch {
                                        db.pageDao().updateAudioPath(page.id, audioFile.absolutePath)
                                    }
                                    Toast.makeText(context, "Audio recorded: ${duration / 1000}s", Toast.LENGTH_SHORT).show()
                                } else {
                                    val success = audioRecordManager.startRecording(audioFile)
                                    if (success) {
                                        isAudioRecording = true
                                        Toast.makeText(context, "Recording audio...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Audio permission needed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            if (isAudioRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Record Audio",
                                    tint = if (hasAudioFile) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }

                        IconButton(onClick = { showTemplateDialog = true }) {
                            Icon(Icons.Default.DashboardCustomize, contentDescription = "Template")
                        }

                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export to PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    saveCurrentPageStrokes()
                                    scope.launch {
                                        Toast.makeText(context, "Generating multi-page PDF...", Toast.LENGTH_SHORT).show()
                                        val allPages = db.pageDao().getPagesListForNotebook(notebookId)
                                        val pagesData = allPages.map { p ->
                                            val t = PageTemplate.fromString(p.templateType, p.backgroundColor)
                                            val s = strokeConverter.toStrokesList(p.strokeJson)
                                            t to s
                                        }
                                        val outFile = File(context.getExternalFilesDir(null), "${notebookTitle.replace(" ", "_")}.pdf")
                                        val success = pdfManager.exportToPdf(outFile, pagesData)
                                        if (success) {
                                            Toast.makeText(context, "PDF saved to: ${outFile.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (isStylusOnlyMode) "Stylus Palm Rejection: ON" else "Stylus Palm Rejection: OFF") },
                                leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null) },
                                onClick = {
                                    isStylusOnlyMode = !isStylusOnlyMode
                                    canvasRef?.isStylusOnlyMode = isStylusOnlyMode
                                    showOverflowMenu = false
                                    val msg = if (isStylusOnlyMode) "Stylus palm rejection ON (finger only pans)" else "Finger drawing enabled"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Audio Recording & Playback Bar
                if (isAudioRecording) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val sec = (audioRecordingDurationMs / 1000) % 60
                            val min = (audioRecordingDurationMs / 1000) / 60
                            Text(
                                "Recording Audio: %02d:%02d (Syncing strokes)".format(min, sec),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }
                } else if (hasAudioFile) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val page = currentPage ?: return@IconButton
                                    val audioFile = File(context.getExternalFilesDir(null), "audio_${page.id}.m4a")

                                    if (isAudioPlaying) {
                                        audioRecordManager.pausePlayback()
                                        isAudioPlaying = false
                                    } else {
                                        val started = audioRecordManager.startPlayback(
                                            audioFile = audioFile,
                                            onProgress = { cur, tot ->
                                                audioPlaybackProgress = cur
                                                audioPlaybackTotal = tot
                                                isAudioPlaying = true
                                            },
                                            onComplete = {
                                                isAudioPlaying = false
                                                audioPlaybackProgress = 0
                                            }
                                        )
                                        if (started) isAudioPlaying = true
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Audio Play/Pause",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            val curSec = (audioPlaybackProgress / 1000) % 60
                            val curMin = (audioPlaybackProgress / 1000) / 60
                            val totSec = (audioPlaybackTotal / 1000) % 60
                            val totMin = (audioPlaybackTotal / 1000) / 60

                            Text(
                                "%02d:%02d / %02d:%02d".format(curMin, curSec, totMin, totSec),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Slider(
                                value = audioPlaybackProgress.toFloat(),
                                onValueChange = {
                                    audioPlaybackProgress = it.toInt()
                                    audioRecordManager.seekTo(it.toInt())
                                },
                                valueRange = 0f..(audioPlaybackTotal.toFloat().coerceAtLeast(1f)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isMarkdownMode) {
                // Real Milkdown Markdown Editor View
                AndroidView(
                    factory = { ctx ->
                        MilkdownEditorView(ctx).apply {
                            setMarkdown("# $notebookTitle\n\nStart typing rich-text markdown notes here...")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // High-Performance Vector Canvas
                AndroidView(
                    factory = { ctx ->
                        DrawingCanvasView(ctx).apply {
                            canvasRef = this
                            this.isStylusOnlyMode = isStylusOnlyMode
                            this.isAutoShapeEnabled = isAutoShapeEnabled
                            this.isScribbleToEraseEnabled = isScribbleToEraseEnabled
                            this.isEraserAutoDeselect = isEraserAutoDeselect
                            this.onUndoRedoStateChanged = { u, r ->
                                canUndo = u
                                canRedo = r
                            }
                            this.onStrokeChangedListener = {
                                hasLassoSelection = this.selectedStrokes.isNotEmpty()
                                saveCurrentPageStrokes()
                            }
                            this.onTextToolTapped = { x, y ->
                                textEntryCoord = x to y
                                showTextEntryDialog = true
                            }
                            this.onColorSampled = { sampledColor ->
                                selectedColor = sampledColor
                                selectedToolType = previousPenTool
                                Toast.makeText(context, "Sampled Color", Toast.LENGTH_SHORT).show()
                            }
                            this.onEraserFinished = {
                                selectedToolType = previousPenTool
                            }
                            this.currentAudioTimestampMsProvider = {
                                if (audioRecordManager.isRecording) audioRecordManager.getRecordingElapsedMs() else null
                            }
                            this.onStrokeTapped = { stroke ->
                                stroke.audioTimestampMs?.let { ms ->
                                    audioRecordManager.seekTo(ms.toInt())
                                    Toast.makeText(context, "Seek audio to ${ms / 1000}s", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Notewise Toolbox at bottom
                FloatingToolbox(
                    selectedTool = selectedToolType,
                    selectedPenType = selectedPenType,
                    selectedColor = selectedColor,
                    selectedPenWidth = selectedPenWidth,
                    selectedPenOpacity = selectedPenOpacity,
                    selectedHighlighterWidth = selectedHighlighterWidth,
                    selectedHighlighterColor = selectedHighlighterColor,
                    selectedEraserRadius = selectedEraserRadius,
                    isStrokeEraser = isStrokeEraser,
                    isEraserAutoDeselect = isEraserAutoDeselect,
                    isAutoShapeEnabled = isAutoShapeEnabled,
                    isScribbleToEraseEnabled = isScribbleToEraseEnabled,
                    selectedShapeType = selectedShapeType,
                    isRulerEnabled = isRulerEnabled,
                    selectedRulerType = selectedRulerType,
                    hasLassoSelection = hasLassoSelection,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onSelectTool = { tool ->
                        selectedToolType = tool
                        updateCanvasTool()
                    },
                    onPenTypeChange = { pt ->
                        selectedPenType = pt
                        updateCanvasTool()
                    },
                    onColorChange = { col ->
                        selectedColor = col
                        updateCanvasTool()
                    },
                    onPenWidthChange = { w ->
                        selectedPenWidth = w
                        updateCanvasTool()
                    },
                    onPenOpacityChange = { op ->
                        selectedPenOpacity = op
                        updateCanvasTool()
                    },
                    onHighlighterWidthChange = { w ->
                        selectedHighlighterWidth = w
                        updateCanvasTool()
                    },
                    onHighlighterColorChange = { col ->
                        selectedHighlighterColor = col
                        updateCanvasTool()
                    },
                    onEraserRadiusChange = { r ->
                        selectedEraserRadius = r
                        updateCanvasTool()
                    },
                    onToggleStrokeEraser = { se ->
                        isStrokeEraser = se
                        updateCanvasTool()
                    },
                    onToggleEraserAutoDeselect = {
                        isEraserAutoDeselect = !isEraserAutoDeselect
                        canvasRef?.isEraserAutoDeselect = isEraserAutoDeselect
                    },
                    onToggleAutoShape = {
                        isAutoShapeEnabled = !isAutoShapeEnabled
                        canvasRef?.isAutoShapeEnabled = isAutoShapeEnabled
                    },
                    onToggleScribbleToErase = {
                        isScribbleToEraseEnabled = !isScribbleToEraseEnabled
                        canvasRef?.isScribbleToEraseEnabled = isScribbleToEraseEnabled
                    },
                    onShapeTypeChange = { st ->
                        selectedShapeType = st
                        updateCanvasTool()
                    },
                    onToggleRuler = {
                        isRulerEnabled = !isRulerEnabled
                        canvasRef?.rulerGuide?.isEnabled = isRulerEnabled
                        canvasRef?.invalidate()
                    },
                    onRulerTypeChange = { rt ->
                        selectedRulerType = rt
                        canvasRef?.rulerGuide?.rulerType = rt
                        canvasRef?.invalidate()
                    },
                    onLassoDelete = {
                        canvasRef?.deleteSelectedStrokes()
                        hasLassoSelection = false
                    },
                    onLassoDuplicate = {
                        canvasRef?.duplicateSelectedStrokes()
                    },
                    onLassoRecolor = { color ->
                        canvasRef?.recolorSelectedStrokes(color)
                    },
                    onLassoLockToggle = {
                        val canvas = canvasRef ?: return@FloatingToolbox
                        val anyUnlocked = canvas.selectedStrokes.any { !it.isLocked }
                        canvas.lockSelectedStrokes(anyUnlocked)
                        hasLassoSelection = false
                        Toast.makeText(context, if (anyUnlocked) "Locked selected strokes" else "Unlocked", Toast.LENGTH_SHORT).show()
                    },
                    onOpenStickerLibrary = {
                        showStickerSheet = true
                    },
                    onClearCanvas = {
                        canvasRef?.clearCanvas()
                    },
                    onUndo = {
                        canvasRef?.undo()
                    },
                    onRedo = {
                        canvasRef?.redo()
                    },
                    onResetZoom = {
                        canvasRef?.resetZoom()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                )
            }
        }
    }

    // Template Picker Dialog
    if (showTemplateDialog) {
        val currentTpl = canvasRef?.currentTemplate ?: PageTemplate.Ruled()
        TemplateSelectionDialog(
            current = currentTpl,
            onSelect = { newTpl ->
                canvasRef?.currentTemplate = newTpl
                canvasRef?.invalidate()
                showTemplateDialog = false
                currentPage?.let { page ->
                    scope.launch {
                        val hex = String.format("#%06X", (0xFFFFFF and newTpl.backgroundColor))
                        db.pageDao().updateTemplate(page.id, newTpl.name, hex)
                    }
                }
            },
            onDismiss = { showTemplateDialog = false }
        )
    }

    // Page Overview Bottom Sheet
    if (showPageOverviewSheet) {
        PageOverviewSheet(
            pages = pages,
            currentPageIndex = currentPageIndex,
            onSelectPage = { idx ->
                saveCurrentPageStrokes()
                currentPageIndex = idx
            },
            onAddPage = {
                saveCurrentPageStrokes()
                scope.launch {
                    val newIdx = pages.size
                    val newPage = PageEntity(
                        notebookId = notebookId,
                        pageIndex = newIdx,
                        templateType = currentPage?.templateType ?: "Ruled",
                        backgroundColor = currentPage?.backgroundColor ?: "#FFFFFF"
                    )
                    db.pageDao().upsertPage(newPage)
                    db.notebookDao().updatePageCount(notebookId, pages.size + 1)
                    currentPageIndex = newIdx
                    showPageOverviewSheet = false
                }
            },
            onDuplicatePage = { idx ->
                saveCurrentPageStrokes()
                val targetPage = pages.getOrNull(idx) ?: return@PageOverviewSheet
                scope.launch {
                    val newIdx = pages.size
                    val dupPage = PageEntity(
                        notebookId = notebookId,
                        pageIndex = newIdx,
                        templateType = targetPage.templateType,
                        backgroundColor = targetPage.backgroundColor,
                        strokeJson = targetPage.strokeJson
                    )
                    db.pageDao().upsertPage(dupPage)
                    db.notebookDao().updatePageCount(notebookId, pages.size + 1)
                    currentPageIndex = newIdx
                    showPageOverviewSheet = false
                }
            },
            onDeletePage = { idx ->
                val targetPage = pages.getOrNull(idx) ?: return@PageOverviewSheet
                if (pages.size > 1) {
                    scope.launch {
                        db.pageDao().permanentlyDeletePage(targetPage.id)
                        db.notebookDao().updatePageCount(notebookId, pages.size - 1)
                        if (currentPageIndex >= pages.size - 1) {
                            currentPageIndex = (pages.size - 2).coerceAtLeast(0)
                        }
                    }
                }
            },
            onDismiss = { showPageOverviewSheet = false }
        )
    }

    // Sticker & Component Library Sheet
    if (showStickerSheet) {
        StickerLibrarySheet(
            stickers = stickers,
            hasLassoSelection = hasLassoSelection,
            onSaveCurrentSelection = { name, tag ->
                val canvas = canvasRef ?: return@StickerLibrarySheet
                val json = strokeConverter.fromStrokesList(canvas.selectedStrokes)
                scope.launch {
                    val sticker = StickerEntity(name = name, tag = tag, strokeJson = json)
                    db.stickerDao().insertSticker(sticker)
                    Toast.makeText(context, "Saved to Library as '$name'!", Toast.LENGTH_SHORT).show()
                }
            },
            onSelectSticker = { sticker ->
                val stickerStrokes = strokeConverter.toStrokesList(sticker.strokeJson)
                canvasRef?.insertSticker(stickerStrokes, 300f, 400f)
                Toast.makeText(context, "Inserted '${sticker.name}'", Toast.LENGTH_SHORT).show()
            },
            onDeleteSticker = { id ->
                scope.launch { db.stickerDao().deleteSticker(id) }
            },
            onDismiss = { showStickerSheet = false }
        )
    }

    // Text Insertion Dialog
    if (showTextEntryDialog) {
        AlertDialog(
            onDismissRequest = {
                showTextEntryDialog = false
                textInput = ""
            },
            title = { Text("Add Text Annotation") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type text here...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coord = textEntryCoord
                        if (coord != null && textInput.isNotBlank()) {
                            canvasRef?.addTextAnnotation(
                                text = textInput,
                                x = coord.first,
                                y = coord.second,
                                fontSize = 24f,
                                color = selectedColor
                            )
                        }
                        showTextEntryDialog = false
                        textInput = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTextEntryDialog = false
                        textInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
