package com.notewise.clone.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.notewise.clone.core.canvas.DrawingPath
import com.notewise.clone.core.canvas.DrawingTool
import com.notewise.clone.core.template.PageTemplate
import com.notewise.clone.core.template.TemplateRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfManager(private val context: Context) {
    private val templateRenderer = TemplateRenderer()

    /**
     * Renders a specific page of an imported PDF as a Bitmap background
     */
    suspend fun renderPdfPage(uri: Uri, pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Exports canvas notes to a standard vector PDF file
     */
    suspend fun exportToPdf(
        outputFile: File,
        pagesData: List<Pair<PageTemplate, List<DrawingPath>>>,
        pageWidth: Int = 1600,
        pageHeight: Int = 2200
    ): Boolean = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val penPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        try {
            for ((index, pageData) in pagesData.withIndex()) {
                val (template, strokes) = pageData
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)
                templateRenderer.render(canvas, pageWidth.toFloat(), pageHeight.toFloat(), template)

                // Vector strokes
                for (stroke in strokes) {
                    if (stroke.tool is DrawingTool.Eraser || stroke.tool is DrawingTool.Lasso) continue
                    penPaint.color = stroke.color
                    penPaint.strokeWidth = stroke.strokeWidth
                    canvas.drawPath(stroke.getPath(), penPaint)
                }

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document.close()
        }
    }
}

