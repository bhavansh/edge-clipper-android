package dev.bmg.edgeclip.service

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.widget.HorizontalScrollView
import android.graphics.Color
import dev.bmg.edgeclip.R
import dev.bmg.edgeclip.data.ClipEntity
import dev.bmg.edgeclip.data.ClipRepository
import dev.bmg.edgeclip.data.ClipType
import dev.bmg.edgeclip.data.SettingsManager
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
    private val settings = SettingsManager(context)
    private var currentFilter = "ALL" // ALL, URL, PHONE, OTP, IMAGE

    fun refreshPanel(container: LinearLayout, clips: List<ClipEntity>) {
        container.removeAllViews()
        
        // 1. Add Filter Chips & Pause Button
        container.addView(buildHeaderRow())
        
        val filteredClips = when (currentFilter) {
            "ALL" -> clips
            "IMAGE" -> clips.filter { it.type == ClipType.IMAGE }
            else -> clips.filter { it.subtype == currentFilter }
        }

        if (filteredClips.isEmpty()) {
            container.addView(buildEmptyState())
            return
        }

        filteredClips.forEach { clip ->
            container.addView(buildClipBlock(clip))
            container.addView(buildBlockDivider())
        }
    }

    private fun buildHeaderRow(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(8))
        
        // Filter Chips (Scrollable)
        val scroll = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isHorizontalScrollBarEnabled = false
        }
        
        val chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val filters = listOf("ALL", "URL", "PHONE", "OTP", "IMAGE")
        filters.forEach { filter ->
            val chip = TextView(context).apply {
                text = filter
                textSize = 10f
                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                val isSelected = currentFilter == filter
                setTextColor(if (isSelected) Color.WHITE else ContextCompat.getColor(context, R.color.text_primary))
                background = pill(if (isSelected) ContextCompat.getColor(context, R.color.purple_500) else ContextCompat.getColor(context, R.color.button_clear_bg))
                
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.rightMargin = dpToPx(6) }
                
                setOnClickListener {
                    currentFilter = filter
                    EdgeClipService.instance?.triggerRefresh()
                }
            }
            chipContainer.addView(chip)
        }
        
        scroll.addView(chipContainer)
        addView(scroll)
        
        // Pause Button
        val pauseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_pause)
            val isPaused = settings.isPaused
            setColorFilter(if (isPaused) ContextCompat.getColor(context, R.color.pill_text_danger) else ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            setOnClickListener {
                settings.isPaused = !settings.isPaused
                setImageResource(R.drawable.ic_pause)
                setColorFilter(if (settings.isPaused) ContextCompat.getColor(context, R.color.pill_text_danger) else ContextCompat.getColor(context, R.color.text_primary))
            }
        }
        addView(pauseBtn)
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
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            
            // Re-adding the actual content rendering
            when (clip.type) {
                ClipType.TEXT -> {
                    addView(TextView(context).apply {
                        text = clip.text
                        textSize = 13f
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        setLineSpacing(0f, 1.2f)
                        setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                    })
                }
                ClipType.IMAGE -> {
                    val path = clip.imagePath
                    if (path != null) {
                        addView(buildImageView(path))
                    }
                }
            }

            // Full-width action bar at the bottom
            addView(buildActionBar(clip))
        }
        
        addView(contentLayout)
    }

    private fun buildActionBar(clip: ClipEntity): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(38)
        )
        setBackgroundColor(ContextCompat.getColor(context, R.color.action_bar_bg))
        gravity = Gravity.CENTER_VERTICAL

        // 1. Contextual Action Icon
        if (clip.subtype != "NONE") {
            val actionBtn = ImageView(context).apply {
                val iconRes = when (clip.subtype) {
                    "URL" -> R.drawable.ic_web
                    "PHONE" -> R.drawable.ic_call
                    "OTP" -> R.drawable.ic_check
                    else -> 0
                }
                if (iconRes != 0) {
                    setImageResource(iconRes)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setColorFilter(ContextCompat.getColor(context, R.color.purple_500))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    setOnClickListener { performAction(clip) }
                }
            }
            if (actionBtn.drawable != null) {
                addView(actionBtn)
                addView(View(context).apply { 
                    layoutParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(20))
                    setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
                })
            }
        }

        // 2. Copy Button (Icon)
        val copyBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_copy)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val tint = ContextCompat.getColor(context, R.color.text_primary)
            setColorFilter(tint)
            
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (clip.type == ClipType.TEXT) onCopyText(clip.text!!) else onCopyImage(clip)
                
                setImageResource(R.drawable.ic_check)
                setColorFilter(ContextCompat.getColor(context, R.color.pill_text_success))
                
                postDelayed({
                    setImageResource(R.drawable.ic_copy)
                    setColorFilter(tint)
                }, 1500)
            }
        }

        // Vertical divider between Copy and Delete
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(20))
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
        }

        // Delete Button (Icon)
        val deleteBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setColorFilter(ContextCompat.getColor(context, R.color.pill_text_danger))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onDelete(clip)
            }
        }

        addView(copyBtn)
        addView(divider)
        addView(deleteBtn)
    }

    private fun performAction(clip: ClipEntity) {
        val text = clip.text ?: return
        try {
            val intent = when (clip.subtype) {
                "URL" -> Intent(Intent.ACTION_VIEW, Uri.parse(text))
                "PHONE" -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:$text"))
                "OTP" -> {
                    // Extract digits for OTP specifically
                    val digits = Regex("\\d{4,8}").find(text)?.value ?: text
                    onCopyText(digits)
                    Toast.makeText(context, "OTP Copied", Toast.LENGTH_SHORT).show()
                    null
                }
                else -> null
            }
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            Log.e("PanelUI", "Action failed", e)
        }
    }

    private fun buildImageView(path: String): ImageView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(140)
        )
        scaleType = ImageView.ScaleType.CENTER_CROP
        
        scope.launch(Dispatchers.IO) {
            val bmp = try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
            if (bmp != null) {
                withContext(Dispatchers.Main) { setImageBitmap(bmp) }
            }
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
