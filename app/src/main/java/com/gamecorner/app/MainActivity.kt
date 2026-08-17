package com.gamecorner.app

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var selectedMode = LaunchMode.GAME_SPACE
    private var selectedGamePackage: String? = null
    private lateinit var permissionPrompter: SystemPermissionPromptController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.TRANSPARENT
        permissionPrompter = SystemPermissionPromptController(this)

        val gameSpace = findViewById<View>(R.id.gameSpaceMode)
        val directOpen = findViewById<View>(R.id.directOpenMode)
        gameSpace.setOnClickListener {
            selectedMode = LaunchMode.GAME_SPACE
            gameSpace.isSelected = true
            directOpen.isSelected = false
            animateSelection(gameSpace)
        }
        directOpen.setOnClickListener {
            selectedMode = LaunchMode.DIRECT_OPEN
            gameSpace.isSelected = false
            directOpen.isSelected = true
            animateSelection(directOpen)
        }
        gameSpace.isSelected = true

        findViewById<Button>(R.id.addGameButton).setOnClickListener {
            showAddGameDialog()
        }

        findViewById<SeekBar>(R.id.launchSlider).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (progress >= 100) {
                        startActivity(
                            Intent(this@MainActivity, GameDashboardActivity::class.java)
                                .putExtra(GameDashboardActivity.EXTRA_MODE, selectedMode.name)
                                .putExtra(
                                    GameDashboardActivity.EXTRA_GAME_PACKAGE,
                                    selectedGamePackage,
                                ),
                        )
                        seekBar.progress = 0
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (seekBar.progress < 100) seekBar.progress = 0
                }
            },
        )

        loadInstalledGames()
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

    private fun loadInstalledGames() {
        val library = findViewById<LinearLayout>(R.id.gameLibrary)
        library.removeAllViews()

        val installed = launchableApps()
        val installedByPackage = installed.associateBy { it.packageName }
        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedPackages = preferences
            .getStringSet(KEY_SELECTED_GAMES, emptySet())
            .orEmpty()
            .filter { installedByPackage.containsKey(it) }
            .toMutableSet()

        // Free Fire is shown automatically when present; every other game is
        // explicitly selected with ADD GAME.
        if (savedPackages.isEmpty()) {
            installed.firstOrNull {
                packageManager.getApplicationLabel(it).toString()
                    .contains("free fire", ignoreCase = true)
            }?.let { savedPackages.add(it.packageName) }
        }

        if (savedPackages.isEmpty()) {
            findViewById<View>(R.id.gameLibraryEmpty).visibility = View.VISIBLE
            selectedGamePackage = null
            return
        }

        findViewById<View>(R.id.gameLibraryEmpty).visibility = View.GONE
        preferences.edit().putStringSet(KEY_SELECTED_GAMES, savedPackages).apply()
        selectedGamePackage = selectedGamePackage?.takeIf { savedPackages.contains(it) }
            ?: savedPackages.firstOrNull()

        savedPackages.mapNotNull { installedByPackage[it] }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
            .forEach { app ->
                val packageName = app.packageName
                val label = packageManager.getApplicationLabel(app).toString()
                library.addView(
                    gameCard(
                        app,
                        label,
                        "Play time  /  ${playTimeLabel(packageName)}  •  Installed game",
                    ),
                )
            }
    }

    private fun showAddGameDialog() {
        val apps = launchableApps()
        if (apps.isEmpty()) {
            Toast.makeText(this, "No launchable apps were found.", Toast.LENGTH_SHORT).show()
            return
        }

        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val selected = preferences
            .getStringSet(KEY_SELECTED_GAMES, emptySet())
            .orEmpty()
            .toMutableSet()
        val labels = apps.map { packageManager.getApplicationLabel(it).toString() }.toTypedArray()
        val checked = apps.map { selected.contains(it.packageName) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("ADD GAME TO OPTIMIZE")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val packageName = apps[which].packageName
                if (isChecked) selected.add(packageName) else selected.remove(packageName)
            }
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE GAMES") { _, _ ->
                preferences.edit().putStringSet(KEY_SELECTED_GAMES, selected).apply()
                selectedGamePackage = selected.firstOrNull()
                loadInstalledGames()
                Toast.makeText(
                    this,
                    "${selected.size} game(s) added.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            .show()
    }

    private fun launchableApps(): List<ApplicationInfo> =
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { info ->
                info.packageName != packageName &&
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    packageManager.getLaunchIntentForPackage(info.packageName) != null
            }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }

    private fun playTimeLabel(packageName: String): String {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP ||
            !hasUsageAccess()
        ) {
            return "Usage Access required"
        }
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val stats = (getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)
            .queryAndAggregateUsageStats(start, end)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(
            stats[packageName]?.totalTimeInForeground ?: 0L,
        )
        return if (minutes < 1) {
            "< 1m today"
        } else {
            val hours = minutes / 60
            val remainder = minutes % 60
            "${DecimalFormat("00").format(hours)}h ${
                DecimalFormat("00").format(remainder)
            }m today"
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        return appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName,
        ) == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun gameCard(app: ApplicationInfo, title: String, subtitle: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundResource(R.drawable.bg_cyberpunk_panel)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedGamePackage = app.packageName
                Toast.makeText(
                    this@MainActivity,
                    "$title selected for optimization.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(50, 50)
            setImageDrawable(packageManager.getApplicationIcon(app))
            contentDescription = title
        }
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = 12
            }
        }
        copy.addView(TextView(this).apply {
            text = title.uppercase()
            setTextColor(getColor(R.color.gc_text))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        copy.addView(TextView(this).apply {
            text = subtitle
            setTextColor(getColor(R.color.gc_muted))
            textSize = 11f
            setPadding(0, 5, 0, 0)
        })
        card.addView(icon)
        card.addView(copy)
        return card
    }

    private fun animateSelection(view: View) {
        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80L)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(160L).start()
            }
            .start()
    }

    private enum class LaunchMode {
        GAME_SPACE,
        DIRECT_OPEN,
    }

    companion object {
        private const val PREFS_NAME = "game_corner_preferences"
        private const val KEY_SELECTED_GAMES = "selected_games"
    }
}