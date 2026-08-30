package com.speakscreen.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import java.util.Locale

class SpeakService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var tts: TextToSpeech
    private var isFloatingViewAdded = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        setupFloatingWindow()
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_widget, null)

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(floatingView, params)
        isFloatingViewAdded = true

        val btnPlay = floatingView.findViewById<Button>(R.id.btnPlay)
        val btnStop = floatingView.findViewById<Button>(R.id.btnStop)
        val btnClose = floatingView.findViewById<Button>(R.id.btnClose)

        btnPlay.setOnClickListener { readScreenContent() }
        btnStop.setOnClickListener { tts.stop() }
        btnClose.setOnClickListener {
            if (isFloatingViewAdded) {
                windowManager.removeView(floatingView)
                isFloatingViewAdded = false
            }
        }
    }

    private fun readScreenContent() {
        val rootNode = rootInActiveWindow ?: return
        val sb = StringBuilder()
        extractText(rootNode, sb)
        
        if (sb.isNotEmpty()) {
            tts.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun extractText(node: AccessibilityNodeInfo?, sb: java.lang.StringBuilder) {
        if (node == null) return
        if (node.text != null) {
            sb.append(node.text.toString()).append(" . ")
        }
        if (node.contentDescription != null) {
            sb.append(node.contentDescription.toString()).append(" . ")
        }
        for (i in 0 until node.childCount) {
            extractText(node.getChild(i), sb)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    
    override fun onInterrupt() { tts.stop() }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US // အင်္ဂလိပ်စာဖတ်ရန်
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFloatingViewAdded) windowManager.removeView(floatingView)
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
