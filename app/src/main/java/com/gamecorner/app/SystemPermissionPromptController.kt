package com.gamecorner.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Presents required system permissions one at a time. Special permissions
 * open Android's own settings screens; runtime notification permission uses the
 * native prompt. A permission is only reminded once per foreground session so
 * the user never receives a stack of dialogs.
 */
class SystemPermissionPromptController(
    private val activity: AppCompatActivity,
) {
    private val promptedThisSession = mutableSetOf<String>()

    fun onResume() {
        activity.window.decorView.postDelayed({ promptNext() }, 260L)
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == REQUEST_NOTIFICATIONS) {
            activity.window.decorView.postDelayed({ promptNext() }, 260L)
        }
    }

    fun remindOverlayPermission() {
        promptedThisSession.remove(KEY_OVERLAY)
        promptNext()
    }

    private fun promptNext() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            promptedThisSession.add(KEY_NOTIFICATIONS)
        ) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
            return
        }

        if (!Settings.canDrawOverlays(activity) &&
            promptedThisSession.add(KEY_OVERLAY)
        ) {
            showSettingsPrompt(
                key = KEY_OVERLAY,
                title = "Cấp quyền menu rời",
                message = "GAME CORNER cần quyền hiển thị trên ứng dụng khác để menu HUD nổi xuất hiện trong game.",
                action = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
            return
        }

        if (!hasUsageAccess() && promptedThisSession.add(KEY_USAGE)) {
            showSettingsPrompt(
                key = KEY_USAGE,
                title = "Cấp quyền thống kê thời gian chơi",
                message = "Cho phép GAME CORNER đọc thời gian ứng dụng ở chế độ nền để hiển thị play time chính xác.",
                action = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            )
        }
    }

    private fun showSettingsPrompt(
        key: String,
        title: String,
        message: String,
        action: Intent,
    ) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Mở cài đặt") { _, _ ->
                activity.startActivity(action)
            }
            .setNegativeButton("Để sau", null)
            .setOnDismissListener {
                // Keep the key marked for this foreground session; on the next
                // app launch it will be reminded again if still missing.
            }
            .show()
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = activity.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        return appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            activity.packageName,
        ) == android.app.AppOpsManager.MODE_ALLOWED
    }

    companion object {
        const val REQUEST_NOTIFICATIONS = 15201
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_OVERLAY = "overlay"
        private const val KEY_USAGE = "usage"
    }
}