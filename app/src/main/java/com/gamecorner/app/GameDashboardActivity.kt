package com.gamecorner.app

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class GameDashboardActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var bootSound: MediaPlayer? = null
    private lateinit var permissionPrompter: SystemPermissionPromptController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_dashboard)
        permissionPrompter = SystemPermissionPromptController(this)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )

        val video = findViewById<VideoView>(R.id.bootVideo)
        val dashboard = findViewById<View>(R.id.dashboardContent)
        video.setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.rog_boot_anim_1080p}"))
        bootSound = MediaPlayer.create(this, R.raw.rog_sound_effects)
        bootSound?.setVolume(0.72f, 0.72f)
        bootSound?.start()
        video.setOnPreparedListener { player ->
            player.isLooping = false
            video.start()
        }
        video.setOnCompletionListener {
            showDashboard(video, dashboard)
        }
        video.setOnErrorListener { _, _, _ ->
            showDashboard(video, dashboard)
            true
        }
        handler.postDelayed({ showDashboard(video, dashboard) }, BOOT_DURATION_MS)

        findViewById<Button>(R.id.launchGameButton).setOnClickListener {
            launchSelectedGame()
        }
        findViewById<Button>(R.id.hudButton).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                permissionPrompter.remindOverlayPermission()
            } else {
                startHud()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionPrompter.isInitialized) permissionPrompter.onResume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionPrompter.onRequestPermissionsResult(requestCode)
    }

    private fun showDashboard(video: VideoView, dashboard: View) {
        if (dashboard.visibility == View.VISIBLE) return
        video.stopPlayback()
        video.visibility = View.GONE
        dashboard.visibility = View.VISIBLE
    }

    private fun launchSelectedGame() {
        val requestedPackage = intent.getStringExtra(EXTRA_GAME_PACKAGE)
        val requestedIntent = requestedPackage?.let { packageManager.getLaunchIntentForPackage(it) }
        val detected = packageManager.getInstalledApplications(0)
            .firstOrNull {
                packageManager.getApplicationLabel(it).toString().contains("free fire", true) &&
                    packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
        val launchIntent = requestedIntent ?: detected?.let { packageManager.getLaunchIntentForPackage(it.packageName) }
        if (launchIntent == null) {
            Toast.makeText(this, "Select a game from ADD GAME first.", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(launchIntent)
            startHud()
        }
    }

    private fun startHud() {
        androidx.core.content.ContextCompat.startForegroundService(
            this,
            Intent(this, OverlayService::class.java),
        )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        bootSound?.release()
        bootSound = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "launch_mode"
        const val EXTRA_GAME_PACKAGE = "game_package"
        private const val BOOT_DURATION_MS = 3800L
    }
}