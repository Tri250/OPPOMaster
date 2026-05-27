package com.omaster.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.omaster.app.R
import com.omaster.app.model.Preset
import com.omaster.app.util.FlashNoteHelper

class OneTapFlashNoteButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cardView: CardView
    private var iconView: ImageView
    private var labelView: TextView
    private var preset: Preset? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_one_tap_flash_note, this, true)

        cardView = findViewById(R.id.card_flash_note)
        iconView = findViewById(R.id.icon_flash_note)
        labelView = findViewById(R.id.label_flash_note)

        setupClickListener()
    }

    fun setPreset(preset: Preset) {
        this.preset = preset
        updateUI(preset)
    }

    fun setLabel(label: String) {
        labelView.text = label
    }

    fun setIconResource(resId: Int) {
        iconView.setImageResource(resId)
    }

    fun setOnSaveSuccessListener(listener: () -> Unit) {
        cardView.setOnClickListener {
            preset?.let { p ->
                saveToFlashNote(p, listener)
            }
        }
    }

    private fun setupClickListener() {
        cardView.setOnClickListener {
            preset?.let { p ->
                saveToFlashNote(p)
            }
        }
    }

    private fun saveToFlashNote(preset: Preset, onSuccess: () -> Unit = {}) {
        FlashNoteHelper.quickSavePreset(
            context = context,
            preset = preset,
            onSuccess = {
                animateSuccess()
                onSuccess()
            },
            onError = { error ->
                showError(error)
            }
        )
    }

    private fun updateUI(preset: Preset) {
        labelView.text = "一键闪记"
    }

    private fun animateSuccess() {
        iconView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                iconView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun showError(message: String) {
        labelView.text = message
        labelView.postDelayed({
            labelView.text = "一键闪记"
        }, 2000)
    }
}
