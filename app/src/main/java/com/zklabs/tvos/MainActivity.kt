package com.zklabs.tvos

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.ResolveInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/** No HOME interception, accessibility service, boot receiver, network or tracking. */
class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("zk_settings", MODE_PRIVATE) }
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var time: TextView
    private var page = "Home"
    private var apps: List<App> = emptyList()
    private var scanning = false
    private var resumed = false
    private var wallpaper: Bitmap? = null
    private var wallpaperLoaded = false
    private var scanGeneration = 0
    private val cyan = Color.rgb(0, 229, 255)
    private val violet = Color.rgb(155, 120, 255)
    private val white = Color.rgb(239, 246, 255)
    private val muted = Color.rgb(157, 174, 197)
    private val accent get() = if (prefs.getString("theme", "Cyan") == "Violet") violet else cyan
    private data class App(val pkg: String, val label: String, val info: ResolveInfo)
    private val ticker = object : Runnable {
        override fun run() {
            if (::time.isInitialized) time.text = SimpleDateFormat(
                if (prefs.getBoolean("clock24", false)) "HH:mm  ·  EEE, d MMM" else "h:mm a  ·  EEE, d MMM", Locale.getDefault()
            ).format(Date())
            if (resumed) handler.postDelayed(this, 60000 - System.currentTimeMillis() % 60000)
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        runCatching { dataDir() }
        page = state?.getString("page") ?: "Home"
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        render()
    }
    override fun onSaveInstanceState(out: Bundle) { out.putString("page", page); super.onSaveInstanceState(out) }
    override fun onResume() {
        super.onResume(); resumed = true
        handler.removeCallbacks(ticker); ticker.run(); discover()
    }
    override fun onPause() { resumed = false; handler.removeCallbacks(ticker); super.onPause() }
    override fun onDestroy() { scanGeneration++; worker.shutdownNow(); handler.removeCallbacksAndMessages(null); super.onDestroy() }
    override fun onBackPressed() {
        if (page != "Home") { page = "Home"; render() }
        else AlertDialog.Builder(this).setTitle("Return to Fire TV?").setMessage("Amazon Home remains available at all times.")
            .setPositiveButton("Amazon Home") { _, _ -> amazonHome() }.setNegativeButton("Stay here", null).show()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.repeatCount == 0) { page = "Settings"; render(); return true }
        return super.onKeyDown(keyCode, event)
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 16f, color: Int = white) = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color); fontFeatureSettings = "kern"
    }
    private fun panel(focus: Boolean = false): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, if (focus) intArrayOf(0xFF173C4E.toInt(), 0xFF26203F.toInt()) else intArrayOf(0xE61A2234.toInt(), 0xE6101523.toInt())
    ).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(if (focus) 2 else 1), if (focus) accent else 0xFF2A3547.toInt()) }
    private fun interactive(view: View, action: () -> Unit) {
        view.isFocusable = true; view.isClickable = true; view.background = panel()
        view.setOnClickListener { action() }
        view.setOnFocusChangeListener { v, focused ->
            v.background = panel(focused)
            val scale = if (focused) 1.035f else 1f
            if (prefs.getBoolean("motion", true)) v.animate().scaleX(scale).scaleY(scale).setDuration(130).start()
            else { v.animate().cancel(); v.scaleX = 1f; v.scaleY = 1f }
            v.elevation = dp(if (focused) 8 else 0).toFloat()
        }
    }
    private fun button(label: String, action: () -> Unit): TextView = text(label, 14f).apply {
        gravity = Gravity.CENTER; setPadding(dp(18), dp(12), dp(18), dp(12)); interactive(this, action)
    }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; clipChildren = false; clipToPadding = false }
    private fun col() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false }
    private fun addGap(parent: LinearLayout, height: Int = 14) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun render() {
        val focusedTag = currentFocus?.tag
        root = FrameLayout(this)
        root.addView(Aurora(), FrameLayout.LayoutParams(-1, -1))
        val shell = col().apply { setPadding(dp(38), dp(24), dp(38), dp(16)) }
        root.addView(shell, FrameLayout.LayoutParams(-1, -1)); setContentView(root)
        val header = row()
        header.addView(text("ZK", 30f, accent).apply { typeface = Typeface.DEFAULT_BOLD })
        header.addView(text("  TV OS 2030\n  ZK LABS", 11f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        time = text("", 14f); header.addView(time)
        shell.addView(header); addGap(shell, 18)
        val nav = row()
        listOf("Home", "App library", "Settings").forEach { name ->
            val b = button(name) { page = name; render() }.apply { tag = "nav:$name"; if (page == name) setTextColor(accent) }
            nav.addView(b, LinearLayout.LayoutParams(-2, dp(46)).apply { rightMargin = dp(10) })
        }
        nav.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        nav.addView(button("Amazon Home ↗") { amazonHome() }.apply { tag = "amazon" })
        shell.addView(nav); addGap(shell, 16)
        val scroll = ScrollView(this).apply { isFillViewport = false; clipToPadding = false }
        content = col().apply { setPadding(dp(5), dp(5), dp(5), dp(16)) }
        scroll.addView(content); shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        when(page) { "App library" -> library(); "Settings" -> settings(); else -> home() }
        val footer = row()
        footer.addView(text("D-pad to explore  ·  Hold OK on an app for favorites  ·  ☰ Settings", 10f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        footer.addView(text("by TechBaseML", 11f, accent))
        shell.addView(footer)
        handler.removeCallbacks(ticker); ticker.run()
        val previous = focusedTag?.let { root.findViewWithTag<View>(it) }
        (previous ?: root.findViewWithTag<View>("nav:$page"))?.requestFocus()
    }
    private fun home() {
        val hero = col().apply { background = panel(); setPadding(dp(26), dp(18), dp(26), dp(18)) }
        hero.addView(text("YOUR SPACE. REIMAGINED.", 11f, accent))
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hero.addView(text(when(hour) { in 5..11 -> "Good morning."; in 12..16 -> "Good afternoon."; else -> "Good evening." }, 34f).apply { typeface = Typeface.create("sans-serif-light", Typeface.NORMAL) })
        hero.addView(text("A calmer home for everything you love.", 14f, muted))
        content.addView(hero); addGap(content, 20)
        content.addView(text("Your favorites", 20f)); addGap(content, 12)
        val favorites = favoritePackages().mapNotNull { pkg -> apps.find { it.pkg == pkg } }
        if (favorites.isEmpty()) content.addView(text(if (scanning) "Finding installed apps…" else "Open App library and hold OK on any app to pin it here.", 14f, muted))
        else appStrip(favorites)
        addGap(content, 18); content.addView(text("Watch something great", 20f)); addGap(content, 12)
        val shortcuts = row()
        listOf("YouTube" to listOf("com.amazon.firetv.youtube", "com.google.android.youtube.tv"), "Netflix" to listOf("com.netflix.ninja", "com.netflix.mediaclient"), "JioHotstar" to listOf("in.startv.hotstar", "com.hotstar.android")).forEach { (label, packages) ->
            val app = apps.find { it.pkg in packages } ?: apps.find { it.label.contains(label, true) || (label == "JioHotstar" && it.label.contains("hotstar", true)) }
            val b = button(if (app != null) "$label  ↗" else "$label\nNot installed") {
                if (app != null) launch(app) else AlertDialog.Builder(this).setTitle("$label is not installed").setMessage("Install $label from Amazon Appstore, then return here. Availability depends on your region and device.").setPositiveButton("Open Amazon Home") { _, _ -> amazonHome() }.setNegativeButton("Close", null).show()
            }
            shortcuts.addView(b, LinearLayout.LayoutParams(0, dp(68), 1f).apply { rightMargin = dp(12) })
        }
        content.addView(shortcuts)
    }
    private fun appStrip(items: List<App>) {
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; setPadding(dp(3), dp(6), dp(3), dp(8)) }
        val line = row(); items.forEach { line.addView(appCard(it), LinearLayout.LayoutParams(dp(160), dp(112)).apply { rightMargin = dp(14) }) }
        horizontal.addView(line); content.addView(horizontal)
    }
    private fun appCard(app: App): View {
        val card = col().apply { gravity = Gravity.CENTER; setPadding(dp(10), dp(10), dp(10), dp(10)); tag = "app:${app.pkg}" }
        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            try { setImageDrawable(app.info.loadIcon(packageManager)) } catch (_: Exception) { setImageResource(com.zklabs.tvos.R.drawable.zk_icon) }
        }
        card.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48))); addGap(card, 8)
        card.addView(text(app.label, 13f).apply { gravity = Gravity.CENTER; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END })
        interactive(card) { launch(app) }
        card.setOnLongClickListener { favoriteDialog(app); true }
        return card
    }
    private fun library() {
        val title = row(); title.addView(text("App library  ·  ${apps.size}", 24f), LinearLayout.LayoutParams(0, -2, 1f))
        title.addView(button("Refresh") { discover(force = true) }); content.addView(title); addGap(content)
        if (apps.isEmpty()) content.addView(text(if(scanning) "Finding installed apps…" else "No launchable apps found. Try Refresh.", 15f, muted))
        apps.chunked(5).forEach { group ->
            val line = row()
            group.forEach { line.addView(appCard(it), LinearLayout.LayoutParams(0, dp(116), 1f).apply { setMargins(dp(5), dp(5), dp(5), dp(5)) }) }
            repeat(5 - group.size) { line.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
            content.addView(line)
        }
    }
    private fun discover(force: Boolean = false) {
        if (scanning) return
        scanning = true
        if (force || apps.isEmpty()) render()
        val generation = ++scanGeneration
        worker.execute {
            val result = runCatching {
                val found = LinkedHashMap<String, App>()
                listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).forEach { category ->
                    packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).forEach { info ->
                        val p = info.activityInfo.packageName
                        if (p != packageName && info.activityInfo.enabled && info.activityInfo.exported && !found.containsKey(p)) found[p] = App(p, info.loadLabel(packageManager).toString(), info)
                    }
                }
                found.values.sortedBy { it.label.lowercase(Locale.ROOT) }
            }
            handler.post {
                if (isDestroyed || generation != scanGeneration) return@post
                scanning = false
                result.onSuccess { fresh ->
                    val changed = fresh.map { it.pkg to it.label } != apps.map { it.pkg to it.label }
                    apps = fresh
                    if (!prefs.contains("favorites")) {
                        val first = fresh.filter { it.label.contains("youtube", true) || it.label.contains("netflix", true) || it.label.contains("hotstar", true) }.map { it.pkg }
                        saveFavorites(first)
                    }
                    if (changed || force || apps.isEmpty()) render()
                }.onFailure { toast("Could not read apps: ${it.message}"); render() }
            }
        }
    }
    private fun favoritePackages(): List<String> = try {
        val a = JSONArray(prefs.getString("favorites", "[]")); (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) { emptyList() }
    private fun saveFavorites(list: List<String>) { prefs.edit().putString("favorites", JSONArray(list).toString()).apply() }
    private fun favoriteDialog(app: App) {
        val favorites = favoritePackages().toMutableList(); val pinned = app.pkg in favorites
        val actions = if (pinned) arrayOf("Launch", "Remove from favorites", "Move to first") else arrayOf("Launch", "Add to favorites")
        AlertDialog.Builder(this).setTitle(app.label).setItems(actions) { _, n ->
            when (n) {
                0 -> launch(app)
                1 -> { if (pinned) favorites.remove(app.pkg) else favorites.add(app.pkg); saveFavorites(favorites); render() }
                2 -> { favorites.remove(app.pkg); favorites.add(0, app.pkg); saveFavorites(favorites); render() }
            }
        }.setNegativeButton("Cancel", null).show()
    }
    private fun launch(app: App) {
        try { startActivity(Intent(Intent.ACTION_MAIN).setComponent(ComponentName(app.pkg, app.info.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("This app could not open. Refresh App library and try again.") }
    }
    private fun amazonHome() {
        try { startActivity(Intent().setComponent(ComponentName("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.HomeActivity_vNext")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            catch (_: Exception) { toast("Press the remote HOME button to return to Fire TV.") }
        }
    }
    private fun setting(title: String, detail: String, action: () -> Unit) {
        val box = col().apply { setPadding(dp(18), dp(12), dp(18), dp(12)); tag = "setting:$title" }
        box.addView(text(title, 16f)); box.addView(text(detail, 12f, muted))
        interactive(box, action); content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
    private fun settings() {
        content.addView(text("Make it yours.", 28f)); addGap(content)
        setting("Accent color", prefs.getString("theme", "Cyan")!!) {
            choose("Accent color", arrayOf("Cyan", "Violet"), "theme")
        }
        setting("Wallpaper", prefs.getString("wallpaper", "Aurora")!!) {
            AlertDialog.Builder(this).setTitle("Wallpaper").setItems(arrayOf("Aurora", "Midnight", "Violet sky", "Custom image")) { _, i ->
                if (i == 3) loadCustomWallpaper() else { prefs.edit().putString("wallpaper", arrayOf("Aurora", "Midnight", "Violet sky")[i]).apply(); render() }
            }.setNegativeButton("Cancel", null).show()
        }
        setting("Clock format", if(prefs.getBoolean("clock24", false)) "24-hour" else "12-hour") { prefs.edit().putBoolean("clock24", !prefs.getBoolean("clock24", false)).apply(); render() }
        setting("Focus animations", if(prefs.getBoolean("motion", true)) "On · 130 ms" else "Off") { prefs.edit().putBoolean("motion", !prefs.getBoolean("motion", true)).apply(); render() }
        setting("Export settings", "Save favorites, theme, clock and animation settings to a JSON file") { exportSettings() }
        setting("Restore settings", "Read zk-settings.json from the app's external files folder") { restoreSettings() }
        setting("Original Fire TV", "Open the preserved Amazon interface") { amazonHome() }
        setting("HOME button support", "System-controlled · single and triple HOME redirection is not enabled") {
            message("HOME behavior", "Fire OS handles the HOME key. This build deliberately leaves Amazon Home unchanged. Open ZK from Your Apps & Channels, then use Amazon Home here or the remote HOME key to return. Triple HOME interception is not implemented.")
        }
        setting("About ZK TV OS 2030", "ZK LABS · 0.1.0 device-test · by TechBaseML") {
            message("ZK LABS", "ZK TV OS 2030\nby TechBaseML\ngithub.com/techbaseml/zk-tv-os-2030\n\nOffline core. No ads, tracking, network permission or background service. Native gradient panels approximate glass without expensive live blur.\n\nOptional weather is not included in this build. Real-device acceptance tests are pending.")
        }
    }
    private fun choose(title: String, options: Array<String>, key: String) {
        AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(options, options.indexOf(prefs.getString(key, options[0]))) { d, i ->
            prefs.edit().putString(key, options[i]).apply(); d.dismiss(); render()
        }.setNegativeButton("Cancel", null).show()
    }
    private fun dataDir(): File = getExternalFilesDir(null) ?: throw IllegalStateException("External files storage is unavailable")
    private fun loadCustomWallpaper() {
        val file = try { File(dataDir(), "wallpaper.jpg") } catch (e: Exception) { message("Wallpaper", e.message ?: "Storage unavailable"); return }
        if (!file.exists()) {
            message("Custom wallpaper", "Copy a JPEG to this path with ADB, then select Custom image again:\n\n${file.absolutePath}\n\nImages are downsampled to keep memory usage low."); return
        }
        worker.execute {
            val result = runCatching {
                require(file.length() <= 20L * 1024 * 1024) { "Image must be smaller than 20 MB" }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
                var sample = 1
                while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1080) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }) ?: error("Cannot decode image")
            }
            handler.post {
                if (isDestroyed) return@post
                result.onSuccess { wallpaper = it; wallpaperLoaded = true; prefs.edit().putString("wallpaper", "Custom image").apply(); render() }
                    .onFailure { message("Wallpaper could not load", it.message ?: "Invalid image") }
            }
        }
    }
    private fun exportSettings() {
        try {
            val json = JSONObject().put("schema", 1).put("theme", prefs.getString("theme", "Cyan"))
                .put("wallpaper", prefs.getString("wallpaper", "Aurora")).put("clock24", prefs.getBoolean("clock24", false))
                .put("motion", prefs.getBoolean("motion", true)).put("favorites", JSONArray(favoritePackages()))
            val file = File(dataDir(), "zk-settings.json")
            val tmp = File(dataDir(), "zk-settings.json.tmp"); tmp.writeText(json.toString(2))
            check(tmp.renameTo(file)) { "Cannot save export" }
            message("Settings exported", "Saved to:\n${file.absolutePath}\n\nCopy this file to your phone with adb pull before uninstalling. A custom wallpaper must be copied separately. This is a settings backup, not a firmware backup.")
        } catch (e: Exception) { message("Export failed", e.message ?: "Storage unavailable") }
    }
    private fun restoreSettings() {
        try {
            val file = File(dataDir(), "zk-settings.json")
            require(file.isFile && file.length() <= 65536) { "Place a valid zk-settings.json (under 64 KB) in ${dataDir()}" }
            val j = JSONObject(file.readText()); require(j.getInt("schema") == 1) { "Unsupported backup version" }
            val theme = j.getString("theme"); val wall = j.getString("wallpaper")
            require(theme in listOf("Cyan", "Violet")) { "Invalid theme" }
            require(wall in listOf("Aurora", "Midnight", "Violet sky", "Custom image")) { "Invalid wallpaper" }
            val clock = j.getBoolean("clock24"); val motion = j.getBoolean("motion"); val items = j.getJSONArray("favorites")
            require(items.length() <= 200) { "Too many favorites" }
            val favorites = (0 until items.length()).map { items.getString(it) }
            require(favorites.all { it.matches(Regex("[a-zA-Z0-9_.]+")) }) { "Invalid app identifier" }
            AlertDialog.Builder(this).setTitle("Restore settings?").setMessage("Replace current theme, favorites, clock and animation preferences with this backup?")
                .setPositiveButton("Restore") { _, _ ->
                    prefs.edit().putString("theme", theme).putString("wallpaper", if (wall == "Custom image" && !File(dataDir(), "wallpaper.jpg").exists()) "Aurora" else wall)
                        .putBoolean("clock24", clock).putBoolean("motion", motion).putString("favorites", JSONArray(favorites.distinct()).toString()).apply()
                    wallpaperLoaded = false; render(); toast("Settings restored")
                }.setNegativeButton("Cancel", null).show()
        } catch(e: Exception) { message("Restore failed", e.message ?: "Invalid backup") }
    }
    private fun message(title: String, value: String) { AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("OK", null).show() }
    private fun toast(value: String) { Toast.makeText(this, value, Toast.LENGTH_LONG).show() }
    private inner class Aurora : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        init {
            if (prefs.getString("wallpaper", "Aurora") == "Custom image" && !wallpaperLoaded) {
                wallpaperLoaded = true; post { if (!isDestroyed) loadCustomWallpaper() }
            }
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas); canvas.drawColor(0xFF05070D.toInt())
            val style = prefs.getString("wallpaper", "Aurora")
            val image = wallpaper
            if (style == "Custom image" && image != null) {
                val scale = maxOf(width.toFloat() / image.width, height.toFloat() / image.height)
                val w = image.width * scale; val h = image.height * scale
                paint.shader = null; canvas.drawBitmap(image, null, RectF((width-w)/2, (height-h)/2, (width+w)/2, (height+h)/2), paint)
                canvas.drawColor(0xB305070D.toInt()); return
            }
            if (style == "Midnight" || width == 0) return
            paint.shader = RadialGradient(width * 0.83f, height * 0.12f, width * 0.65f, intArrayOf(if(style == "Violet sky") 0xFF382056.toInt() else 0xFF173849.toInt(), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = RadialGradient(width * 0.1f, height * 0.95f, width * 0.5f, 0xFF231743.toInt(), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint); paint.shader = null
        }
    }
}
