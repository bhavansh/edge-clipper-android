package dev.bmg.edgeclip.service

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.core.content.ContextCompat
import dev.bmg.edgeclip.R
import dev.bmg.edgeclip.data.ClipEntity
import dev.bmg.edgeclip.data.ClipRepository
import dev.bmg.edgeclip.data.ClipType
import kotlinx.coroutines.*

class PanelUIManager(
    private val context: Context,
    private val repository: ClipRepository,
    private val scope: CoroutineScope,
    private val onCopyText: (String) -> Unit,
    private val onCopyImage: (ClipEntity) -> Unit,
    private val onDelete: (ClipEntity) -> Unit,
    private val onClearAll: () -> Unit
) {

    fun refreshPanel(container: LinearLayout, clips: List<ClipEntity>) {
        container.removeAllViews()

        if (clips.isEmpty()) {
            container.addView(buildEmptyState())
            return
        }

        clips.forEach { clip ->
            container.addView(buildClipBlock(clip))
            container.addView(buildBlockDivider())
        }
    }

    private fun buildEmptyState(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        addView(TextView(context).apply {
            text = "No items"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        addView(TextView(context).apply {
            text = "Copied text will appear here"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(6), 0, 0)
        })
    }

    private fun buildClipBlock(clip: ClipEntity): FrameLayout = FrameLayout(context).apply {
        setPadding(dpToPx(14), dpToPx(12), dpToPx(10), dpToPx(10))

        when (clip.type) {
            ClipType.TEXT -> {
                addView(TextView(context).apply {
                    text = clip.text
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setLineSpacing(0f, 1.35f)
                    setPadding(0, 0, 0, dpToPx(26))
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                })
                addView(buildPillRow(clip))
            }

            ClipType.IMAGE -> {
                val path = clip.imagePath
                if (path != null) {
                    addView(buildImageView(path))
                }
                addView(buildImagePillRow(clip))
            }
        }
    }

    private fun buildImageView(path: String): ImageView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dpToPx(140)
        ).also { it.bottomMargin = dpToPx(30) }
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(8).toFloat())
            }
        }

        scope.launch(Dispatchers.IO) {
            val bmp = try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
            if (bmp != null) {
                withContext(Dispatchers.Main) { setImageBitmap(bmp) }
            }
        }
    }

    private fun buildImagePillRow(clip: ClipEntity): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )

        addView(buildCopyImagePill(clip))
        addView(Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1)
        })
        addView(buildDeletePill(clip))
    }

    private fun buildCopyImagePill(clip: ClipEntity): TextView = TextView(context).apply {
        text = "Copy"
        textSize = 11.5f
        setTextColor(ContextCompat.getColor(context, R.color.pill_text))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        background = pill(ContextCompat.getColor(context, R.color.pill_bg))
        setPadding(dpToPx(12), dpToPx(5), dpToPx(12), dpToPx(5))

        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onCopyImage(clip)
            text = "Copied ✓"
            setTextColor(ContextCompat.getColor(context, R.color.pill_text_success))
            background = pill(ContextCompat.getColor(context, R.color.pill_bg_success))
            postDelayed({
                text = "Copy"
                setTextColor(ContextCompat.getColor(context, R.color.pill_text))
                background = pill(ContextCompat.getColor(context, R.color.pill_bg))
            }, 1800)
        }
    }

    private fun buildPillRow(clip: ClipEntity): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )

        addView(buildCopyPill(clip.text!!))
        addView(Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1)
        })
        addView(buildDeletePill(clip))
    }

    private fun buildCopyPill(clipText: String): TextView = TextView(context).apply {
        text = "Copy"
        textSize = 11.5f
        setTextColor(ContextCompat.getColor(context, R.color.pill_text))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        background = pill(ContextCompat.getColor(context, R.color.pill_bg))
        setPadding(dpToPx(12), dpToPx(5), dpToPx(12), dpToPx(5))

        setOnClickListener {
            onCopyText(clipText)
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            text = "Copied ✓"
            setTextColor(ContextCompat.getColor(context, R.color.pill_text_success))
            background = pill(ContextCompat.getColor(context, R.color.pill_bg_success))
            postDelayed({
                text = "Copy"
                setTextColor(ContextCompat.getColor(context, R.color.pill_text))
                background = pill(ContextCompat.getColor(context, R.color.pill_bg))
            }, 1800)
        }
    }

    private fun buildDeletePill(clip: ClipEntity): TextView = TextView(context).apply {
        text = "✕"
        textSize = 11.5f
        setTextColor(ContextCompat.getColor(context, R.color.pill_text_danger))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        background = pill(ContextCompat.getColor(context, R.color.pill_bg_danger))
        setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))

        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onDelete(clip)
        }
    }

    private fun buildBlockDivider(): View = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
        )
    }

    fun buildClearButton(): TextView = TextView(context).apply {
        text = "Clear All"
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.button_clear_text))
        background = pill(ContextCompat.getColor(context, R.color.button_clear_bg))
        setPadding(dpToPx(24), dpToPx(7), dpToPx(24), dpToPx(7))

        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { lp ->
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.topMargin = dpToPx(10)
        }

        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClearAll()
            text = "Cleared"
            postDelayed({ text = "Clear All" }, 1500)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun pill(color: Int) =
        GradientDrawable().apply { setColor(color); cornerRadius = 999f }
}
