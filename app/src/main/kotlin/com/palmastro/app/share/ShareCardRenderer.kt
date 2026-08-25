package com.palmastro.app.share

import android.graphics.*

/**
 * Renders share cards as bitmaps (PRD 13.7): no palm imagery, watermark/brand included,
 * no raw sensitive data. All user-visible text is injected by the calling composable so
 * cards follow the app locale — this object never hardcodes user-facing copy.
 */
object ShareCardRenderer {

    private const val CARD_WIDTH = 1080
    private const val CORNER_RADIUS = 24f
    private const val HEADER_HEIGHT = 120f
    private const val FOOTER_HEIGHT = 60f
    private const val PADDING = 48f

    // Footer disclaimer: the safety note travels with the card once it leaves the app.
    private const val DISCLAIMER_TEXT_SIZE = 20f
    private const val DISCLAIMER_LINE_HEIGHT = 26f
    private const val DISCLAIMER_MAX_LINES = 3
    private const val DISCLAIMER_GAP = 24f
    private const val WATERMARK_BASELINE = 20f

    private val headerColorStart = Color.parseColor("#5E35B1")
    private val headerColorEnd = Color.parseColor("#7E57C2")

    // Light-theme PalmAstroExtendedColors grade values (cards render on white):
    // one brand palette everywhere, calm by design — no alarm red on an outward-
    // facing surface (PRD 12.3 / Theme.kt PalmAstroExtendedColors).
    private val gradeColors = mapOf(
        "Growing" to Color.parseColor("#2E7D32"),
        "Stable" to Color.parseColor("#00695C"),
        "Building" to Color.parseColor("#5E35B1"),
        "Watchout" to Color.parseColor("#B35A00"),
    )

    /** Localized labels resolved via string resources by the calling composable. */
    data class CardLabels(
        val analysis: String,
        val actions: String,
        val reflection: String,
        val watermark: String,
        /** Safety note printed above the watermark; blank renders no footer text. */
        val disclaimer: String = "",
    )

    data class SummaryData(
        val headerTitle: String,
        val monthKey: String,
        val grade: String,
        val gradeDisplay: String,
        val confidenceLine: String,
        val domains: List<DomainScore>,
    )

    data class DomainScore(
        val displayName: String,
        val score: Int,
        val grade: String,
    )

    data class DomainDetailData(
        val headerTitle: String,
        val score: Int,
        val grade: String,
        val gradeDisplay: String,
        val interpretation: String,
        val actionToday: String,
        val prompt: String,
    )

    fun renderSummaryCard(data: SummaryData, labels: CardLabels): Bitmap {
        val discLines = disclaimerLines(labels.disclaimer)
        val bodyHeight = 60f + data.domains.size * 70f + 40f
        val totalHeight = (HEADER_HEIGHT + bodyHeight + footerHeight(discLines)).toInt()
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, totalHeight)
        drawHeader(canvas, data.headerTitle)

        var y = HEADER_HEIGHT + PADDING

        val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gradeColors[data.grade] ?: Color.GRAY
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawCircle(PADDING + 12f, y + 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gradeColors[data.grade] ?: Color.GRAY
        })
        canvas.drawText(data.gradeDisplay, PADDING + 36f, y + 24f, gradePaint)

        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = 28f
        }
        canvas.drawText(data.monthKey, CARD_WIDTH - PADDING - monthPaint.measureText(data.monthKey), y + 24f, monthPaint)

        y += 60f

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 30f
        }
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
        val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
        }

        for (domain in data.domains) {
            canvas.drawText(domain.displayName, PADDING, y + 28f, namePaint)

            scorePaint.color = gradeColors[domain.grade] ?: Color.GRAY
            canvas.drawText("${domain.score}", PADDING + 160f, y + 28f, scorePaint)

            val barLeft = PADDING + 240f
            val barRight = CARD_WIDTH - PADDING
            val barTop = y + 14f
            val barBottom = y + 30f
            canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, 8f, 8f, barBgPaint)

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gradeColors[domain.grade] ?: Color.GRAY
            }
            val fillRight = barLeft + (barRight - barLeft) * (domain.score / 100f)
            canvas.drawRoundRect(barLeft, barTop, fillRight, barBottom, 8f, 8f, fillPaint)

            y += 70f
        }

        val confPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 24f
        }
        canvas.drawText(data.confidenceLine, PADDING, y + 24f, confPaint)

        drawFooter(canvas, totalHeight, labels.watermark, discLines)
        return bitmap
    }

    fun renderDomainDetailCard(data: DomainDetailData, labels: CardLabels): Bitmap {
        val discLines = disclaimerLines(labels.disclaimer)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f }
        val interpLines = wrapText(data.interpretation, textPaint, CARD_WIDTH - PADDING * 2, 3)
        val actionLines = wrapText(data.actionToday, textPaint, CARD_WIDTH - PADDING * 2, 2)
        val promptLines = wrapText(data.prompt, textPaint, CARD_WIDTH - PADDING * 2 - 32f, 3)

        val bodyHeight = 80f +
            40f + interpLines.size * 38f +
            40f + actionLines.size * 38f +
            40f + promptLines.size * 38f + 32f +
            PADDING
        val totalHeight = (HEADER_HEIGHT + bodyHeight + footerHeight(discLines)).toInt()
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, totalHeight)
        drawHeader(canvas, data.headerTitle)

        var y = HEADER_HEIGHT + PADDING

        val scoreColor = gradeColors[data.grade] ?: Color.GRAY
        val bigScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("${data.score}", PADDING, y + 56f, bigScorePaint)

        val gradeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(data.gradeDisplay, PADDING + bigScorePaint.measureText("${data.score}") + 16f, y + 56f, gradeLabelPaint)
        y += 80f

        y = drawSection(canvas, labels.analysis, interpLines, y)
        y = drawSection(canvas, labels.actions, actionLines, y)

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5E35B1")
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(labels.reflection, PADDING, y + 20f, sectionPaint)
        y += 36f

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3E5F5")
        }
        val boxHeight = promptLines.size * 38f + 24f
        canvas.drawRoundRect(PADDING, y, CARD_WIDTH - PADDING, y + boxHeight, 12f, 12f, boxPaint)

        val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A148C")
            textSize = 28f
        }
        var py = y + 30f
        for (line in promptLines) {
            canvas.drawText(line, PADDING + 16f, py, promptPaint)
            py += 38f
        }

        drawFooter(canvas, totalHeight, labels.watermark, discLines)
        return bitmap
    }

    private fun drawBackground(canvas: Canvas, height: Int) {
        canvas.drawColor(Color.WHITE)
        val path = Path()
        val rect = RectF(0f, 0f, CARD_WIDTH.toFloat(), height.toFloat())
        path.addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawColor(Color.WHITE)
    }

    private fun drawHeader(canvas: Canvas, title: String) {
        val gradient = LinearGradient(
            0f, 0f, CARD_WIDTH.toFloat(), HEADER_HEIGHT,
            headerColorStart, headerColorEnd,
            Shader.TileMode.CLAMP,
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), HEADER_HEIGHT, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(title, PADDING, HEADER_HEIGHT / 2 + 14f, titlePaint)
    }

    /**
     * The safety note as it will be drawn. Wrapped, because the copy runs well past one
     * line at a readable size in every locale — a single drawText would centre it to a
     * negative x and slide it off the card.
     */
    private fun disclaimerLines(disclaimer: String): List<String> =
        if (disclaimer.isBlank()) {
            emptyList()
        } else {
            wrapText(disclaimer, disclaimerPaint(), CARD_WIDTH - PADDING * 2, DISCLAIMER_MAX_LINES)
        }

    /** Footer grows with the wrapped note so it always stays inside the clipped card. */
    private fun footerHeight(disclaimerLines: List<String>): Float =
        FOOTER_HEIGHT + disclaimerLines.size * DISCLAIMER_LINE_HEIGHT

    // #767676 measures 4.5:1 on the card's white ground; the lighter watermark grey
    // does not, and this line has to be readable wherever the card is reposted.
    private fun disclaimerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#767676")
        textSize = DISCLAIMER_TEXT_SIZE
    }

    private fun drawFooter(canvas: Canvas, totalHeight: Int, watermark: String, disclaimerLines: List<String>) {
        val discPaint = disclaimerPaint()
        var dy = totalHeight - WATERMARK_BASELINE - DISCLAIMER_GAP -
            (disclaimerLines.size - 1) * DISCLAIMER_LINE_HEIGHT
        for (line in disclaimerLines) {
            canvas.drawText(line, (CARD_WIDTH - discPaint.measureText(line)) / 2, dy, discPaint)
            dy += DISCLAIMER_LINE_HEIGHT
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAAAAA")
            textSize = 22f
        }
        val x = (CARD_WIDTH - footerPaint.measureText(watermark)) / 2
        canvas.drawText(watermark, x, totalHeight - WATERMARK_BASELINE, footerPaint)
    }

    private fun drawSection(canvas: Canvas, header: String, lines: List<String>, startY: Float): Float {
        var y = startY
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5E35B1")
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(header, PADDING, y + 20f, sectionPaint)
        y += 36f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 28f
        }
        for (line in lines) {
            canvas.drawText(line, PADDING, y + 24f, textPaint)
            y += 38f
        }
        y += 4f
        return y
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty() && lines.size < maxLines) {
            val count = paint.breakText(remaining, true, maxWidth, null)
            if (lines.size == maxLines - 1 && count < remaining.length) {
                val truncCount = paint.breakText(remaining, true, maxWidth - paint.measureText("…"), null)
                lines.add(remaining.substring(0, truncCount) + "…")
                break
            }
            lines.add(remaining.substring(0, count))
            remaining = remaining.substring(count).trimStart()
        }
        return lines
    }
}
