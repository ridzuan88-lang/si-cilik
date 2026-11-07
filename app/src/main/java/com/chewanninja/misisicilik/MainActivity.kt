package com.chewanninja.misisicilik

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class App(val act: MainActivity) {
    val store = Store(act)
    val chars: List<ChildProfile> = Characters.load(act).also { l -> l.forEach { it.name = store.nameOf(it.id, it.name) } }
    fun cur(): ChildProfile = chars.firstOrNull { it.id == store.selected } ?: chars.first()
    fun data(): PlayerData = store.data(cur().id)
    fun costume(): Costume = COSTUMES.firstOrNull { it.id == data().costume } ?: COSTUMES[0]
    fun award(c: Int, s: Int, xp: Int): List<Badge> = store.award(cur().id, c, s, xp)
}

enum class Screen { Menu, Select, Worlds, Adventure, Hub, Run, Treasure, Balloon, Memory, Maze, Rewards, Settings }

class MainActivity : ComponentActivity() {
    lateinit var gl: GLView
    lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = App(this)
        gl = GLView(this)
        val root = FrameLayout(this)
        root.addView(gl, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val cv = ComposeView(this).apply { setContent { Root(app) } }
        root.addView(cv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowCompat.getInsetsController(window, root)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        Audio.sfxOn = app.store.sfx; Audio.musicOn = app.store.music
    }

    override fun onResume() { super.onResume(); gl.onResume(); Audio.startMusic() }
    override fun onPause() { super.onPause(); gl.onPause(); Audio.pauseMusic() }
}

@Composable
fun Root(app: App) {
    var screen by remember { mutableStateOf(Screen.Menu) }
    var world by remember { mutableIntStateOf(0) }
    var lvl by remember { mutableIntStateOf(0) }
    var showBreak by remember { mutableStateOf(false) }
    val t = app.store.tick.intValue
    val act = app.act
    val menuScene = remember { MenuScene(app.cur(), app.costume()) }

    LaunchedEffect(t) {
        Audio.sfxOn = app.store.sfx; Audio.musicOn = app.store.music; Audio.applyMusic()
        menuScene.profile = app.cur(); menuScene.costume = app.costume()
    }
    LaunchedEffect(screen) {
        val is3d = screen == Screen.Menu || screen == Screen.Adventure
        act.gl.visibility = if (is3d) View.VISIBLE else View.INVISIBLE
        act.gl.renderMode = if (is3d) android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY else android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        act.requestedOrientation = if (screen == Screen.Adventure) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        when (screen) {
            Screen.Menu -> act.gl.renderer.scene = menuScene
            Screen.Adventure -> { }
            else -> act.gl.renderer.scene = null
        }
    }
    LaunchedEffect(Unit) {
        var last = System.currentTimeMillis()
        while (true) {
            delay(30_000)
            val r = app.store.reminder
            if (r > 0 && System.currentTimeMillis() - last >= r * 60_000L) { showBreak = true; last = System.currentTimeMillis() }
        }
    }
    LaunchedEffect(Unit) { while (true) { delay(60_000); app.store.inc(app.cur().id, "minutes") } }

    BackHandler(enabled = screen != Screen.Menu) {
        screen = when (screen) {
            Screen.Run, Screen.Treasure, Screen.Balloon, Screen.Memory, Screen.Maze -> Screen.Hub
            Screen.Adventure -> Screen.Worlds
            else -> Screen.Menu
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260))) togetherWith fadeOut(tween(160)) },
            label = "nav"
        ) { s ->
            when (s) {
                Screen.Menu -> MenuScreen(app) { screen = it }
                Screen.Select -> SelectScreen(app) { screen = Screen.Menu }
                Screen.Worlds -> WorldsScreen(app, onBack = { screen = Screen.Menu }) { w, l -> world = w; lvl = l; screen = Screen.Adventure }
                Screen.Adventure -> AdventureScreen(app, world, lvl, onExit = { screen = Screen.Worlds }, onNext = {
                    if (lvl < 2) lvl++ else if (world < 3) { world++; lvl = 0 } else screen = Screen.Worlds
                })
                Screen.Hub -> HubScreen(app, onBack = { screen = Screen.Menu }) { screen = it }
                Screen.Run -> RunGameScreen(app) { screen = Screen.Hub }
                Screen.Treasure -> TreasureScreen(app) { screen = Screen.Hub }
                Screen.Balloon -> BalloonScreen(app) { screen = Screen.Hub }
                Screen.Memory -> MemoryScreen(app) { screen = Screen.Hub }
                Screen.Maze -> MazeScreen(app) { screen = Screen.Hub }
                Screen.Rewards -> RewardsScreen(app) { screen = Screen.Menu }
                Screen.Settings -> SettingsScreen(app) { screen = Screen.Menu }
            }
        }
        if (showBreak) {
            Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = true) { }, Alignment.Center) {
                Column(Modifier.padding(24.dp).clip(RoundedCornerShape(32.dp)).background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😴 Masa Rehat!", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                    Spacer(Modifier.height(8.dp))
                    Text("Jom minum air dan rehatkan mata sekejap.", fontSize = 20.sp, textAlign = TextAlign.Center, color = Color(0xFF455A64))
                    Spacer(Modifier.height(16.dp))
                    BigButton("Okey!", CGreen, Modifier.fillMaxWidth()) { showBreak = false }
                }
            }
        }
    }
}
