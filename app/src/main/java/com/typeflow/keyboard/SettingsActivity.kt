package com.typeflow.keyboard

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {
    private lateinit var preferences: KeyboardPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = KeyboardPreferences(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(android.graphics.Color.rgb(22, 23, 26))
        }
        root.addView(TextView(this).apply {
            text = "TypeFlow"
            textSize = 30f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Keyboard settings"
            textSize = 16f
            setTextColor(android.graphics.Color.LTGRAY)
            setPadding(0, 0, 0, dp(22))
        })
        root.addView(Switch(this).apply {
            text = "Key vibration"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            isChecked = preferences.hapticsEnabled
            setOnCheckedChangeListener { _, checked -> preferences.hapticsEnabled = checked }
            setPadding(0, dp(8), 0, dp(8))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val heightLabel = TextView(this).apply {
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            text = "Key height: " + preferences.keyHeightDp + " dp"
            setPadding(0, dp(20), 0, 0)
        }
        root.addView(heightLabel)
        root.addView(SeekBar(this).apply {
            max = 24
            progress = preferences.keyHeightDp - 40
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val height = progress + 40
                    heightLabel.text = "Key height: " + height + " dp"
                    if (fromUser) preferences.keyHeightDp = height
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)))
        root.addView(TextView(this).apply {
            text = "TypeFlow Studio · Appearance"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, dp(24), 0, dp(8))
        })
        val colorNames = mapOf(
            KeyboardPreferences.COLOR_GRAPHITE to "Graphite",
            KeyboardPreferences.COLOR_MIDNIGHT to "Midnight",
            KeyboardPreferences.COLOR_OCEAN to "Ocean",
            KeyboardPreferences.COLOR_VIOLET to "Violet"
        )
        val colorButton = Button(this)
        fun refreshColorButton() {
            colorButton.text = "Key color: " + (colorNames[preferences.keyColor] ?: "Graphite") + "  ›"
        }
        colorButton.setOnClickListener {
            val colors = listOf(
                KeyboardPreferences.COLOR_GRAPHITE,
                KeyboardPreferences.COLOR_MIDNIGHT,
                KeyboardPreferences.COLOR_OCEAN,
                KeyboardPreferences.COLOR_VIOLET
            )
            preferences.keyColor = colors[(colors.indexOf(preferences.keyColor) + 1).mod(colors.size)]
            refreshColorButton()
        }
        refreshColorButton()
        root.addView(colorButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val shapeNames = mapOf(
            KeyboardPreferences.SHAPE_ROUNDED to "Rounded",
            KeyboardPreferences.SHAPE_PILL to "Pill",
            KeyboardPreferences.SHAPE_SQUARE to "Square"
        )
        val shapeButton = Button(this)
        fun refreshShapeButton() {
            shapeButton.text = "Key shape: " + (shapeNames[preferences.keyShape] ?: "Rounded") + "  ›"
        }
        shapeButton.setOnClickListener {
            val shapes = listOf(
                KeyboardPreferences.SHAPE_ROUNDED,
                KeyboardPreferences.SHAPE_PILL,
                KeyboardPreferences.SHAPE_SQUARE
            )
            preferences.keyShape = shapes[(shapes.indexOf(preferences.keyShape) + 1).mod(shapes.size)]
            refreshShapeButton()
        }
        refreshShapeButton()
        root.addView(shapeButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(Switch(this).apply {
            text = "Press elevation effect"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            isChecked = preferences.pressEffectEnabled
            setOnCheckedChangeListener { _, checked -> preferences.pressEffectEnabled = checked }
            setPadding(0, dp(8), 0, dp(8))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "Typing stays on-device. TypeFlow does not send keystrokes to a server."
            setTextColor(android.graphics.Color.LTGRAY)
            textSize = 14f
            setPadding(0, dp(18), 0, dp(18))
        })
        root.addView(Button(this).apply {
            text = "Choose / enable keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        setContentView(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
