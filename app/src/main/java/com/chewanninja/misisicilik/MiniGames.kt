package com.chewanninja.misisicilik

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val INKC = 0xFF37474F.toInt()

/* ============================ 1. ADVENTURE RUN ============================ */

class RunState {
    var w = 1f; var h = 1f; var t = 0f
    var py = 0f; var vy = 0f; var dist = 0f
    var coins = 0; var hearts = 3; var invul = 0f; var over = false
    var nextObs = 16f
    val obs = ArrayList<FloatArray>()   // x, width, height (units)
    val cns = ArrayList<FloatArray>()   // x, y, taken
    val score: Int get() = (dist / 2f).toInt() + coins * 10
    fun u() = h / 10f
    fun jump() { if (py <= 0.01f) { vy = 26f; Audio.play("jump") } }
    fun step(dt: Float) {
        t += dt; invul = maxOf(0f, invul - dt)
        val speed = 9f + minOf(t * 0.3f, 10f)
        vy -= 70f * dt; py += vy * dt
        if (py < 0f) { py = 0f; vy = 0f }
        dist += speed * dt
        while (nextObs < dist + 24f) {
            val tall = Random.nextBoolean()
            obs.add(floatArrayOf(nextObs, if (tall) 1.2f else 1.5f, if (tall) 2.6f else 1.6f))
            for (i in -2..2) cns.add(floatArrayOf(nextObs + i * 1.1f, 3.2f + (2 - abs(i)) * 0.5f, 0f))
            val gap = speed * 0.85f + 4f + Random.nextFloat() * speed * 0.6f
            for (i in 0..2) cns.add(floatArrayOf(nextObs + gap / 2f + i * 1.1f - 1.1f, 0.9f, 0f))
            nextObs += gap
        }
        for (o in obs) {
            val rel = o[0] - dist
            if (invul <= 0f && abs(rel) < o[1] / 2f + 0.45f && py < o[2] - 0.15f) {
                hearts--; invul = 1.3f; Audio.play("hit")
                if (hearts <= 0) over = true
            }
        }
        for (c in cns) {
            if (c[2] > 0f) continue
            val rel = c[0] - dist
            if (abs(rel) < 0.9f && abs(c[1] - (py + 0.9f)) < 1.1f) { c[2] = 1f; coins++; Audio.play("coin") }
        }
        obs.removeAll { it[0] - dist < -8f }
        cns.removeAll { it[0] - dist < -8f || it[2] > 0f }
    }
}

@Composable
fun RunGameScreen(app: App, back: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    Box(Modifier.fillMaxSize().background(BgBrush)) {
        if (!started) Intro("Larian Pengembara", "🏃", "Sentuh skrin untuk melompat!\nElak halangan dan kutip syiling.\nAnda ada 3 ❤️.") { started = true }
        else key(round) { RunPlay(app) { outcome = it } }
        outcome?.let { ResultCard(it, "Main Lagi", { outcome = null; round++ }, "Kembali", back) }
        if (outcome == null) Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { BigButton("", COrange, Modifier.size(56.dp), "⬅", pad = 0.dp) { back() } }
    }
}

@Composable
fun RunPlay(app: App, onEnd: (Outcome) -> Unit) {
    val p = app.cur()
    val g = remember { RunState() }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (!g.over) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) g.step(((now - last) / 1e9f).coerceAtMost(0.05f))
                last = now; frame = now
            }
        }
        val coins = g.coins + g.score / 40
        val stars = if (g.score >= 700) 3 else if (g.score >= 400) 2 else if (g.score >= 200) 1 else 0
        app.store.best(p.id, "run_best", g.score)
        val nb = app.award(coins, stars, 10 + g.score / 20)
        onEnd(Outcome("🏁 Tamat!", listOf("Skor: ${g.score}", "🪙 +$coins   ⭐ +$stars"), nb))
    }
    Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { g.jump() } }) {
        val f = frame
        g.w = size.width; g.h = size.height
        val u = g.u(); val gy = size.height * 0.8f; val px0 = size.width * 0.22f
        drawRect(Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFFE1F5FE)), 0f, gy))
        drawCircle(Color(0xFFFFEB3B), u * 1.3f, Offset(size.width * 0.8f, u * 2f))
        for (i in 0 until 4) {
            val hx = ((i * size.width * 0.45f - g.dist * u * 0.15f) % (size.width * 1.8f) + size.width * 1.8f) % (size.width * 1.8f) - size.width * 0.3f
            drawCircle(Color(0xFF81C784), u * 3.2f, Offset(hx, gy + u * 0.8f))
        }
        drawRect(Color(0xFF8D6E63), Offset(0f, gy), Size(size.width, size.height - gy))
        drawRect(Color(0xFF66BB6A), Offset(0f, gy), Size(size.width, u * 0.4f))
        for (o in g.obs) {
            val sx = px0 + (o[0] - g.dist) * u
            drawRoundRect(Color(0xFF8D6E63), Offset(sx - o[1] * u / 2f, gy - o[2] * u), Size(o[1] * u, o[2] * u), CornerRadius(u * 0.4f))
            drawRoundRect(Color(0xFFA1887F), Offset(sx - o[1] * u / 2f + u * 0.15f, gy - o[2] * u + u * 0.15f), Size(o[1] * u * 0.4f, o[2] * u * 0.3f), CornerRadius(u * 0.2f))
        }
        for (c in g.cns) {
            if (c[2] > 0f) continue
            val sx = px0 + (c[0] - g.dist) * u
            if (sx < -u || sx > size.width + u) continue
            drawCircle(Color(0xFFFFC107), u * 0.4f, Offset(sx, gy - c[1] * u))
            drawCircle(Color(0xFFFFE082), u * 0.22f, Offset(sx - u * 0.06f, gy - c[1] * u - u * 0.06f))
        }
        val s = u * 0.55f
        if (g.invul <= 0f || ((g.t * 12f).toInt() % 2 == 0)) drawKid(p, px0, gy - g.py * u - s * 2.35f, s)
        label("❤️ ${g.hearts}   🪙 ${g.coins}   ${g.score}", size.width / 2f, u * 1.2f, u * 0.8f, INKC, Paint.Align.CENTER)
    }
}

/* ============================ 2. TREASURE HUNT ============================ */

class Hunt(val tr: Int, val tc: Int, val clue: String, val tree: Int)

fun makeHunt(round: Int): Hunt {
    val r = Random.nextInt(5); val c = Random.nextInt(5)
    val type = if (round < 2) 1 else Random.nextInt(3)
    if (type == 1) {
        val opts = ArrayList<Triple<String, Int, Int>>()
        if (c < 4) opts.add(Triple("KIRI", r, c + 1))
        if (c > 0) opts.add(Triple("KANAN", r, c - 1))
        if (r < 4) opts.add(Triple("ATAS", r + 1, c))
        if (r > 0) opts.add(Triple("BAWAH", r - 1, c))
        val o = opts[Random.nextInt(opts.size)]
        return Hunt(r, c, "Harta ada di sebelah ${o.first} pokok 🌳", o.second * 5 + o.third)
    }
    val a = Random.nextInt(0, c + 2); val b = c + 1 - a
    if (type == 0) return Hunt(r, c, "Baris ${r + 1}, Lajur $a + $b", -1)
    val k = Random.nextInt(1, 4)
    return Hunt(r, c, "Baris ${r + 1 + k} − $k, Lajur $a + $b", -1)
}

@Composable
fun TreasureScreen(app: App, back: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    Box(Modifier.fillMaxSize().background(BgBrush)) {
        if (!started) Intro("Pemburu Harta", "🗺️", "Baca petunjuk dan sentuh semak untuk cari harta.\n5 pusingan, 3 cubaan setiap pusingan.") { started = true }
        else key(round) { TreasurePlay(app, back) { outcome = it } }
        outcome?.let { ResultCard(it, "Main Lagi", { outcome = null; round++ }, "Kembali", back) }
        if (!started) Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { BigButton("", COrange, Modifier.size(56.dp), "⬅", pad = 0.dp) { back() } }
    }
}

@Composable
fun TreasurePlay(app: App, back: () -> Unit, onEnd: (Outcome) -> Unit) {
    val p = app.cur()
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    var found by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(3) }
    var hunt by remember { mutableStateOf(makeHunt(0)) }
    var hint by remember { mutableStateOf("") }
    var lock by remember { mutableStateOf(false) }
    val cells = remember { mutableStateListOf<Int>().apply { repeat(25) { add(0) } } }   // 0 hidden 1 empty 2 treasure

    fun next() {
        if (round >= 4) {
            val coins = found * 10
            val stars = if (found >= 5) 3 else if (found >= 3) 2 else if (found >= 1) 1 else 0
            app.store.best(p.id, "treasure_best", found)
            val nb = app.award(coins, stars, 10 + found * 6)
            onEnd(Outcome("🗺️ Tamat!", listOf("Harta dijumpai: $found/5", "🪙 +$coins   ⭐ +$stars"), nb))
        } else {
            round++; hunt = makeHunt(round); tries = 3; hint = ""
            for (i in 0 until 25) cells[i] = 0
            lock = false
        }
    }

    fun tap(i: Int) {
        if (lock || cells[i] != 0 || i == hunt.tree) { if (i == hunt.tree) hint = "Itu pokok, bukan harta! 🌳"; return }
        val target = hunt.tr * 5 + hunt.tc
        if (i == target) {
            cells[i] = 2; found++; hint = "Hebat! Jumpa harta! 🎉"; Audio.play("star"); lock = true
            scope.launch { delay(1000); next() }
        } else {
            cells[i] = 1; tries--; Audio.play("hit")
            val dist = abs(i / 5 - hunt.tr) + abs(i % 5 - hunt.tc)
            hint = if (dist <= 1) "Panas! 🔥" else if (dist <= 3) "Suam 🙂" else "Sejuk ❄️"
            if (tries <= 0) {
                cells[target] = 2; hint = "Harta ada di sini! 💰"; lock = true
                scope.launch { delay(1400); next() }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar(app, "Pusingan ${round + 1}/5", back)
        Text(hunt.clue, Modifier.padding(horizontal = 16.dp), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B), textAlign = TextAlign.Center)
        Text("Cubaan: ${"❤️".repeat(tries)}   $hint", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Text("", Modifier.width(28.dp))
            for (c in 1..5) Text("$c", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        for (r in 0 until 5) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).weight(1f)) {
                Text("${r + 1}", Modifier.width(28.dp).align(Alignment.CenterVertically), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                for (c in 0 until 5) {
                    val i = r * 5 + c
                    val label = if (i == hunt.tree) "🌳" else when (cells[i]) { 0 -> "🌿"; 1 -> "🍂"; else -> "💰" }
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(3.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (cells[i] == 2) Color(0xFFFFF59D) else Color(0xFFA5D6A7))
                            .clickable { tap(i) },
                        Alignment.Center
                    ) { Text(label, fontSize = 30.sp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* ============================ 3. BALLOON POP ============================ */

class Balloon(var x: Float, var y: Float, val r: Float, val vy: Float, val c: Color)
class Pop(val x: Float, val y: Float, val c: Color) { var life = 0.4f }

class BalState {
    var w = 1f; var h = 1f; var t = 0f; var left = 45f
    var score = 0; var combo = 0; var lastPop = -9f; var popped = 0; var bestCombo = 0
    private var spawn = 0f
    val bs = ArrayList<Balloon>(); val pops = ArrayList<Pop>()
    private val cols = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFFFF7043))
    fun step(dt: Float) {
        t += dt; left -= dt; spawn -= dt
        val u = h / 10f
        if (spawn <= 0f) {
            spawn = 0.75f / (1f + t / 20f)
            bs.add(Balloon(u * 0.9f + Random.nextFloat() * (w - u * 1.8f), h + u, u * (0.75f + Random.nextFloat() * 0.35f),
                u * (1.8f + Random.nextFloat() * 1.2f) * (1f + t / 60f), cols[Random.nextInt(cols.size)]))
        }
        for (b in bs) b.y -= b.vy * dt
        bs.removeAll { it.y < -2f * u }
        for (p in pops) p.life -= dt
        pops.removeAll { it.life <= 0f }
    }
    fun tap(x: Float, y: Float) {
        for (i in bs.indices.reversed()) {
            val b = bs[i]
            val dx = x - b.x; val dy = y - b.y
            if (dx * dx + dy * dy < (b.r * 1.15f) * (b.r * 1.15f)) {
                combo = if (t - lastPop < 1.2f) combo + 1 else 1
                lastPop = t; bestCombo = maxOf(bestCombo, combo)
                score += 10 * minOf(combo, 5); popped++
                pops.add(Pop(b.x, b.y, b.c)); bs.removeAt(i); Audio.play("pop")
                return
            }
        }
    }
}

@Composable
fun BalloonScreen(app: App, back: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    Box(Modifier.fillMaxSize().background(BgBrush)) {
        if (!started) Intro("Pecah Belon", "🎈", "Sentuh belon untuk memecahkannya!\nPecah cepat-cepat untuk combo.\nMasa: 45 saat.") { started = true }
        else key(round) { BalloonPlay(app) { outcome = it } }
        outcome?.let { ResultCard(it, "Main Lagi", { outcome = null; round++ }, "Kembali", back) }
        if (outcome == null) Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { BigButton("", COrange, Modifier.size(56.dp), "⬅", pad = 0.dp) { back() } }
    }
}

@Composable
fun BalloonPlay(app: App, onEnd: (Outcome) -> Unit) {
    val p = app.cur()
    val g = remember { BalState() }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (g.left > 0f) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) g.step(((now - last) / 1e9f).coerceAtMost(0.05f))
                last = now; frame = now
            }
        }
        val coins = g.score / 15
        val stars = if (g.score >= 650) 3 else if (g.score >= 400) 2 else if (g.score >= 200) 1 else 0
        app.store.best(p.id, "balloon_best", g.score)
        val nb = app.award(coins, stars, 10 + g.score / 15)
        onEnd(Outcome("🎈 Masa Tamat!", listOf("Skor: ${g.score}", "Belon dipecah: ${g.popped}", "Combo terbaik: x${minOf(g.bestCombo, 5)}", "🪙 +$coins   ⭐ +$stars"), nb))
    }
    Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { g.tap(it.x, it.y) } }) {
        val f = frame
        g.w = size.width; g.h = size.height
        val u = g.h / 10f
        for (b in g.bs) {
            drawLine(Color(0x88607D8B), Offset(b.x, b.y + b.r * 1.1f), Offset(b.x + sin(b.y * 0.02f) * u * 0.3f, b.y + b.r * 2.4f), strokeWidth = 3f)
            drawOval(b.c, Offset(b.x - b.r * 0.85f, b.y - b.r), Size(b.r * 1.7f, b.r * 2.1f))
            drawOval(Color(0x66FFFFFF), Offset(b.x - b.r * 0.45f, b.y - b.r * 0.7f), Size(b.r * 0.4f, b.r * 0.6f))
        }
        for (pp in g.pops) drawCircle(pp.c.copy(alpha = pp.life / 0.4f), u * (1.8f - pp.life * 2.5f), Offset(pp.x, pp.y), style = Stroke(6f))
        drawRect(Color(0x55FFFFFF), Offset(0f, 0f), Size(size.width, u * 1.6f))
        label("Skor ${g.score}", u * 0.9f + 56f, u * 1.1f, u * 0.6f, INKC)
        label("⏱ ${g.left.toInt() + 1}", size.width - u * 0.5f, u * 1.1f, u * 0.6f, INKC, Paint.Align.RIGHT)
        if (g.combo >= 2 && g.t - g.lastPop < 1.2f) label("Combo x${minOf(g.combo, 5)}!", size.width / 2f, u * 2.4f, u * 0.9f, 0xFFE91E63.toInt(), Paint.Align.CENTER)
        drawRect(Color(0xFFFFC107), Offset(0f, u * 1.6f), Size(size.width * (g.left / 45f).coerceIn(0f, 1f), u * 0.15f))
    }
}

/* ============================ 4. MEMORY MASTER ============================ */

private val EMOJIS = listOf("🐶", "🐱", "🐼", "🦁", "🐸", "🐵", "🐰", "🦊", "🐯", "🐨")

@Composable
fun MemoryScreen(app: App, back: () -> Unit) {
    var level by remember { mutableIntStateOf(-1) }
    var round by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    Box(Modifier.fillMaxSize().background(BgBrush)) {
        if (level < 0) {
            Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🧠", fontSize = 80.sp)
                Text("Kad Ingatan", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Text("Padankan semua pasangan kad sebelum masa tamat.", fontSize = 20.sp, textAlign = TextAlign.Center, color = Color(0xFF37474F))
                Spacer(Modifier.height(20.dp))
                BigButton("Mudah (60s)", CGreen, Modifier.fillMaxWidth(0.8f)) { level = 0 }
                Spacer(Modifier.height(10.dp))
                BigButton("Sederhana (90s)", COrange, Modifier.fillMaxWidth(0.8f)) { level = 1 }
                Spacer(Modifier.height(10.dp))
                BigButton("Susah (120s)", CRed, Modifier.fillMaxWidth(0.8f)) { level = 2 }
            }
            Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { BigButton("", COrange, Modifier.size(56.dp), "⬅", pad = 0.dp) { back() } }
        } else {
            key(level, round) { MemoryPlay(app, level, back) { outcome = it } }
            outcome?.let { ResultCard(it, "Main Lagi", { outcome = null; round++ }, "Pilih Tahap", { outcome = null; level = -1 }) }
        }
    }
}

@Composable
fun MemoryPlay(app: App, level: Int, back: () -> Unit, onEnd: (Outcome) -> Unit) {
    val p = app.cur()
    val scope = rememberCoroutineScope()
    val cols = intArrayOf(3, 4, 4)[level]; val rows = intArrayOf(4, 4, 5)[level]
    val pairs = cols * rows / 2; val limit = intArrayOf(60, 90, 120)[level]
    val faces = remember { (EMOJIS.take(pairs) + EMOJIS.take(pairs)).shuffled() }
    val up = remember { mutableStateListOf<Boolean>().apply { repeat(cols * rows) { add(false) } } }
    val done = remember { mutableStateListOf<Boolean>().apply { repeat(cols * rows) { add(false) } } }
    var first by remember { mutableIntStateOf(-1) }
    var lock by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(limit) }
    var finished by remember { mutableStateOf(false) }

    fun finish(win: Boolean) {
        if (finished) return
        finished = true
        val matched = done.count { it } / 2
        val score = maxOf(0, matched * 100 + (if (win) left * 5 else 0) - moves * 3)
        val coins = matched * (2 + level) + (if (win) 10 else 0)
        val stars = if (!win) 0 else if (moves <= pairs * 2) 3 else if (moves <= pairs * 3) 2 else 1
        app.store.best(p.id, "memory_best_$level", score)
        if (win && level == 2) app.store.best(p.id, "memory_hard", 1)
        val nb = app.award(coins, stars, 10 + matched * 4)
        onEnd(Outcome(if (win) "🎉 Hebat!" else "⏰ Masa Tamat!", listOf("Skor: $score", "Pasangan: $matched/$pairs   Langkah: $moves", "🪙 +$coins   ⭐ +$stars"), nb, win))
    }
    LaunchedEffect(Unit) { while (left > 0 && !finished) { delay(1000); if (!finished) left-- }; if (!finished) finish(false) }

    fun tap(i: Int) {
        if (lock || finished || up[i] || done[i]) return
        up[i] = true; Audio.play("click")
        if (first < 0) { first = i; return }
        moves++
        val a = first; first = -1
        if (faces[a] == faces[i]) {
            done[a] = true; done[i] = true; Audio.play("coin")
            if (done.all { it }) finish(true)
        } else {
            lock = true
            scope.launch { delay(700); up[a] = false; up[i] = false; lock = false }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(app, "Kad Ingatan", back)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Chip("⏱ $left"); Chip("👣 $moves"); Chip("✅ ${done.count { it } / 2}/$pairs")
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (c in 0 until cols) {
                        val i = r * cols + c
                        val rot by animateFloatAsState(if (up[i] || done[i]) 180f else 0f, tween(250), label = "flip")
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(4.dp)
                                .graphicsLayer { rotationY = rot; cameraDistance = 14f * density }
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (rot > 90f) (if (done[i]) Color(0xFFC8E6C9) else Color.White) else Color(0xFF7E57C2))
                                .clickable { tap(i) },
                            Alignment.Center
                        ) {
                            if (rot > 90f) Text(faces[i], Modifier.graphicsLayer { rotationY = 180f }, fontSize = 36.sp)
                            else Text("❓", fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ============================ 5. MAGIC MAZE ============================ */

class Maze(val n: Int, seed: Int) {
    val walls = IntArray(n * n) { 15 }         // bit0 N, bit1 E, bit2 S, bit3 W (1 = wall)
    init {
        val rnd = Random(seed)
        val seen = BooleanArray(n * n); val st = ArrayList<Int>()
        st.add(0); seen[0] = true
        while (st.isNotEmpty()) {
            val c = st[st.size - 1]; val x = c % n; val y = c / n
            val nb = ArrayList<Int>()
            if (y > 0 && !seen[c - n]) nb.add(0)
            if (x < n - 1 && !seen[c + 1]) nb.add(1)
            if (y < n - 1 && !seen[c + n]) nb.add(2)
            if (x > 0 && !seen[c - 1]) nb.add(3)
            if (nb.isEmpty()) { st.removeAt(st.size - 1); continue }
            val d = nb[rnd.nextInt(nb.size)]
            val nc = when (d) { 0 -> c - n; 1 -> c + 1; 2 -> c + n; else -> c - 1 }
            walls[c] = walls[c] and (1 shl d).inv()
            walls[nc] = walls[nc] and (1 shl ((d + 2) % 4)).inv()
            seen[nc] = true; st.add(nc)
        }
    }
    fun canMove(c: Int, d: Int) = walls[c] and (1 shl d) == 0
    fun step(c: Int, d: Int) = when (d) { 0 -> c - n; 1 -> c + 1; 2 -> c + n; else -> c - 1 }
}

@Composable
fun MazeScreen(app: App, back: () -> Unit) {
    val p = app.cur()
    var started by remember { mutableStateOf(false) }
    var level by remember { mutableIntStateOf(minOf(8, (app.data().stats["maze_level"] ?: 0) + 1)) }
    var round by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    Box(Modifier.fillMaxSize().background(BgBrush)) {
        if (!started) {
            Intro("Maze Ajaib", "🌀", "Seret jari atau tekan anak panah.\nCari bintang ⭐ di hujung maze!\nKutip 3 syiling untuk bonus.") { started = true }
            Box(Modifier.align(Alignment.TopStart).padding(12.dp)) { BigButton("", COrange, Modifier.size(56.dp), "⬅", pad = 0.dp) { back() } }
        } else {
            key(level, round) { MazePlay(app, level, back) { outcome = it } }
            outcome?.let { o ->
                val hasNext = level < 8
                ResultCard(o, if (hasNext) "Tahap Seterusnya ▶" else "Main Semula", { outcome = null; if (hasNext) level++ else round++ }, "Kembali", back)
            }
        }
    }
}

@Composable
fun MazePlay(app: App, level: Int, back: () -> Unit, onEnd: (Outcome) -> Unit) {
    val p = app.cur()
    val n = 4 + level
    val maze = remember { Maze(n, level * 31 + Random.nextInt(1000)) }
    var pos by remember { mutableIntStateOf(0) }
    val coinCells = remember { mutableStateListOf<Int>().apply { val s = HashSet<Int>(); while (s.size < 3) { val c = Random.nextInt(1, n * n - 1); s.add(c) }; addAll(s) } }
    var got by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    fun move(d: Int) {
        if (finished || !maze.canMove(pos, d)) return
        pos = maze.step(pos, d)
        if (coinCells.remove(pos)) { got++; Audio.play("coin") }
        if (pos == n * n - 1) {
            finished = true
            val coins = 8 + 3 * level + got * 3
            val stars = 1 + (if (level >= 4) 1 else 0) + (if (got == 3) 1 else 0)
            app.store.best(p.id, "maze_level", level)
            val nb = app.award(coins, stars, 15 + level * 5)
            onEnd(Outcome("🌟 Jumpa Jalan Keluar!", listOf("Tahap $level selesai", "🪙 +$coins   ⭐ +$stars"), nb))
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar(app, "Maze Tahap $level", back)
        Canvas(
            Modifier.weight(1f).fillMaxWidth().padding(8.dp).pointerInput(level) {
                var ax = 0f; var ay = 0f
                detectDragGestures(onDragStart = { ax = 0f; ay = 0f }) { change, drag ->
                    change.consume()
                    ax += drag.x; ay += drag.y
                    val cs = minOf(size.width, size.height) / n.toFloat()
                    if (abs(ax) > cs * 0.5f || abs(ay) > cs * 0.5f) {
                        val d = if (abs(ax) > abs(ay)) (if (ax > 0) 1 else 3) else (if (ay > 0) 2 else 0)
                        move(d); ax = 0f; ay = 0f
                    }
                }
            }
        ) {
            val cs = minOf(size.width, size.height) / n
            val ox = (size.width - cs * n) / 2f; val oy = (size.height - cs * n) / 2f
            drawRoundRect(Color(0xFFFFF8E1), Offset(ox, oy), Size(cs * n, cs * n), CornerRadius(cs * 0.2f))
            for (c in coinCells) drawCircle(Color(0xFFFFC107), cs * 0.18f, Offset(ox + (c % n + 0.5f) * cs, oy + (c / n + 0.5f) * cs))
            label("⭐", ox + (n - 0.5f) * cs, oy + (n - 0.5f) * cs + cs * 0.28f, cs * 0.75f, 0xFFFFC107.toInt(), Paint.Align.CENTER)
            val sw = maxOf(4f, cs * 0.09f)
            for (c in 0 until n * n) {
                val x = ox + (c % n) * cs; val y = oy + (c / n) * cs; val w = maze.walls[c]
                val col = Color(0xFF5E35B1)
                if (w and 1 != 0) drawLine(col, Offset(x, y), Offset(x + cs, y), sw, StrokeCap.Round)
                if (w and 2 != 0) drawLine(col, Offset(x + cs, y), Offset(x + cs, y + cs), sw, StrokeCap.Round)
                if (w and 4 != 0) drawLine(col, Offset(x, y + cs), Offset(x + cs, y + cs), sw, StrokeCap.Round)
                if (w and 8 != 0) drawLine(col, Offset(x, y), Offset(x, y + cs), sw, StrokeCap.Round)
            }
            val s = cs * 0.2f
            drawKid(p, ox + (pos % n + 0.5f) * cs, oy + (pos / n + 0.5f) * cs - s * 1.1f, s)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            BigButton("", CBlue, Modifier.size(72.dp), "⬅", pad = 0.dp) { move(3) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("", CBlue, Modifier.size(72.dp), "⬆", pad = 0.dp) { move(0) }
                BigButton("", CBlue, Modifier.size(72.dp), "⬇", pad = 0.dp) { move(2) }
            }
            BigButton("", CBlue, Modifier.size(72.dp), "➡", pad = 0.dp) { move(1) }
        }
    }
}
