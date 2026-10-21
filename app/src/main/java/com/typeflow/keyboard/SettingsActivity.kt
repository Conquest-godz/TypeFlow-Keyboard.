package com.typeflow.keyboard

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "TypeFlow Keyboard"
            textSize = 26f
            setPadding(0, 0, 0, dp(12))
        })

        root.addView(TextView(this).apply {
            text = "Alpha baseline — settings controls are not wired yet."
            textSize = 16f
            setPadding(0, 0, 0, dp(24))
        })

        root.addView(Button(this).apply {
            text = "Choose keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })

        root.addView(Button(this).apply {
            text = "Enable TypeFlow"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                startActivity(intent)
            }
        })

        setContentView(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
