package com.chewanninja.misisicilik

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val shadowStyle = TextStyle(shadow = Shadow(Color(0x88000000), Offset(3f, 4f), 8f))

@Composable
fun MenuScreen(app: App, go: (Screen) -> Unit) {
    val t = app.store.tick.intValue
    val p = app.cur(); val d = app.data()
    val inf = rememberInfiniteTransition(label = "logo")
    val bob by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MISI SI CILIK", Modifier.offset(y = (bob * 6).dp), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, style = shadowStyle)
        Text("3D ADVENTURE WORLD", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFEB3B), style = shadowStyle)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.9f))
                .clickable { Audio.play("click"); go(Screen.Select) }.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Portrait(p, 44.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Text("Level ${d.level}  🪙 ${d.coins}  ⭐ ${d.stars}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF546E7A))
            }
            Spacer(Modifier.width(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        BigButton("Main Pengembaraan", CGreen, Modifier.fillMaxWidth(), "▶", fontSize = 26.sp) { go(Screen.Worlds) }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Mini Games", COrange, Modifier.weight(1f), "🎮", pad = 8.dp, fontSize = 18.sp) { go(Screen.Hub) }
            BigButton("Pilih Watak", CPink, Modifier.weight(1f), "🧒", pad = 8.dp, fontSize = 18.sp) { go(Screen.Select) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Ganjaran", CPurple, Modifier.weight(1f), "🏆", pad = 8.dp, fontSize = 18.sp) { go(Screen.Rewards) }
            BigButton("Tetapan", CBlue, Modifier.weight(1f), "⚙️", pad = 8.dp, fontSize = 18.sp) { go(Screen.Settings) }
        }
    }
}

@Composable
fun SelectScreen(app: App, onDone: () -> Unit) {
    val t = app.store.tick.intValue
    var editing by remember { mutableStateOf<ChildProfile?>(null) }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(BgBrush)) {
        TopBar(app, "Pilih Watak", onDone)
        LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(app.chars) { c ->
                val d = app.store.data(c.id)
                val sel = c.id == app.store.selected
                Column(
                    Modifier.clip(RoundedCornerShape(26.dp)).background(if (sel) Color(0xFFFFF59D) else Color.White)
                        .clickable { Audio.play("coin"); app.store.selected = c.id; onDone() }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Portrait(c, 96.dp)
                    Text(c.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B), maxLines = 1)
                    Text("Level ${d.level}  🪙 ${d.coins}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF546E7A))
                    TextButton(onClick = { text = c.name; editing = c }) { Text("✏️ Tukar nama", fontSize = 14.sp) }
                }
            }
        }
    }
    editing?.let { c ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Nama watak") },
            text = { OutlinedTextField(value = text, onValueChange = { if (it.length <= 14) text = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { val n = text.trim(); if (n.isNotEmpty()) { c.name = n; app.store.rename(c.id, n) }; editing = null }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Batal") } }
        )
    }
}

fun levelUnlocked(app: App, w: Int, i: Int): Boolean {
    val n = w * 3 + i
    return n == 0 || "${(n - 1) / 3}-${(n - 1) % 3}" in app.data().levels
}

@Composable
fun WorldsScreen(app: App, onBack: () -> Unit, onPlay: (Int, Int) -> Unit) {
    val t = app.store.tick.intValue
    val d = app.data()
    val cols = listOf(Color(0xFF66BB6A), Color(0xFFBA68C8), Color(0xFFFFA726), Color(0xFF7986CB))
    val icons = listOf("🌳", "🏝️", "🏙️", "🏰")
    Column(Modifier.fillMaxSize().background(BgBrush)) {
        TopBar(app, "Pilih Dunia", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (w in 0..3) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cols[w]).padding(14.dp)) {
                    Text("${icons[w]} Dunia ${w + 1}: ${WORLD_NAMES[w]}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0..2) {
                            val ok = levelUnlocked(app, w, i)
                            val s = d.stats["lv_$w-$i"] ?: 0
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (ok) Color.White else Color(0x66FFFFFF))
                                    .clickable(enabled = ok) { Audio.play("click"); onPlay(w, i) }.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(if (ok) "${i + 1}" else "🔒", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF37474F))
                                Text(if (s > 0) "⭐".repeat(s) else "—", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdventureScreen(app: App, world: Int, idx: Int, onExit: () -> Unit, onNext: () -> Unit) {
    val p = app.cur()
    var attempt by remember { mutableIntStateOf(0) }
    val scene = remember(world, idx, attempt) { AdventureScene(p, app.costume(), world, idx, app.store.quality) }
    DisposableEffect(scene) { app.act.gl.renderer.scene = scene; onDispose { } }
    var hud by remember(scene) { mutableStateOf(scene.hud()) }
    LaunchedEffect(scene) { while (true) { hud = scene.hud(); delay(60) } }
    var outcome by remember(scene) { mutableStateOf<Outcome?>(null) }
    var paused by remember(scene) { mutableStateOf(false) }
    LaunchedEffect(hud.state) {
        if (hud.state == 1 && outcome == null) {
            val stars = maxOf(1, hud.stars)
            val key = "$world-$idx"
            val first = key !in app.data().levels
            val gain = app.store.completeLevel(p.id, key, stars)
            val coins = hud.coins + 10 * stars + (if (first) 25 else 0)
            val nb = app.award(coins, gain, 30 + 10 * stars)
            app.store.inc(p.id, "adv_levels")
            outcome = Outcome("🎉 Tahap Selesai!", listOf("⭐ $stars bintang", "🪙 +$coins syiling") + (if (first) listOf("Bonus tahap pertama +25!") else emptyList()), nb)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("❤️ ${hud.hearts}"); Chip("🪙 ${hud.coins}"); Chip("⭐ ${hud.stars}/3")
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 22.dp).width(160.dp).height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(hud.prog).background(Color(0xFFFFEB3B)))
        }
        BigButton("", COrange, Modifier.align(Alignment.TopEnd).padding(12.dp).size(52.dp), "⏸", pad = 0.dp) { paused = true; scene.paused = true }
        Row(Modifier.align(Alignment.BottomStart).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HoldButton("◀", CBlue, 88.dp) { scene.left = it }
            HoldButton("▶", CBlue, 88.dp) { scene.right = it }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            HoldButton("🏃", COrange, 76.dp) { scene.run = it }
            HoldButton("⬆", CGreen, 100.dp) { if (it) scene.jumpReq = true }
        }
        if (paused) Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = true) { }, Alignment.Center) {
            Column(Modifier.clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏸ Berhenti Sekejap", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Spacer(Modifier.height(12.dp))
                BigButton("Sambung", CGreen, Modifier.width(240.dp)) { paused = false; scene.paused = false }
                Spacer(Modifier.height(8.dp))
                BigButton("Keluar", COrange, Modifier.width(240.dp)) { onExit() }
            }
        }
        if (hud.state == 2) Box(Modifier.fillMaxSize().background(Color(0xAA000000)), Alignment.Center) {
            Column(Modifier.clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😅 Alamak! Cuba lagi!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Spacer(Modifier.height(12.dp))
                BigButton("Cuba Lagi", CGreen, Modifier.width(240.dp)) { attempt++ }
                Spacer(Modifier.height(8.dp))
                BigButton("Keluar", COrange, Modifier.width(240.dp)) { onExit() }
            }
        }
        outcome?.let { o ->
            val hasNext = !(world == 3 && idx == 2)
            ResultCard(o, if (hasNext) "Tahap Seterusnya ▶" else "Selesai", { if (hasNext) onNext() else onExit() }, "Pilih Dunia", onExit)
        }
    }
}

@Composable
fun HubScreen(app: App, onBack: () -> Unit, go: (Screen) -> Unit) {
    val t = app.store.tick.intValue
    val d = app.data()
    class G(val s: Screen, val icon: String, val name: String, val desc: String, val best: String, val c: Color)
    val games = listOf(
        G(Screen.Run, "🏃", "Larian Pengembara", "Lompat halangan, kutip syiling!", "Terbaik: ${d.stats["run_best"] ?: 0}", CGreen),
        G(Screen.Treasure, "🗺️", "Pemburu Harta", "Selesaikan petunjuk, cari harta!", "Terbaik: ${d.stats["treasure_best"] ?: 0}/5", COrange),
        G(Screen.Balloon, "🎈", "Pecah Belon", "Sentuh belon sebelum masa tamat!", "Terbaik: ${d.stats["balloon_best"] ?: 0}", CPink),
        G(Screen.Memory, "🧠", "Kad Ingatan", "Padankan kad yang sama!", "Mudah/Sederhana/Susah", CBlue),
        G(Screen.Maze, "🌀", "Maze Ajaib", "Cari jalan keluar!", "Tahap: ${d.stats["maze_level"] ?: 0}/8", CPurple)
    )
    Column(Modifier.fillMaxSize().background(BgBrush)) {
        TopBar(app, "Mini Games", onBack)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games.size) { i ->
                val g = games[i]
                Row(
                    Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(g.c)
                        .clickable { Audio.play("click"); go(g.s) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(g.icon, fontSize = 52.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(g.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(g.desc, fontSize = 16.sp, color = Color.White)
                        Text(g.best, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF59D))
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsScreen(app: App, onBack: () -> Unit) {
    val t = app.store.tick.intValue
    val p = app.cur(); val d = app.data()
    var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(BgBrush)) {
        TopBar(app, "Ganjaran Saya", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color.White).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Portrait(p, 72.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${p.name} • Level ${d.level}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color(0xFFCFD8DC))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((d.xp % 100) / 100f).background(Color(0xFF66BB6A)))
                    }
                    Text("XP ${d.xp % 100}/100", fontSize = 14.sp, color = Color(0xFF546E7A))
                }
            }
            val avail = app.store.dailyAvailable(p.id)
            BigButton(if (avail) "Ganjaran Harian" else "Datang esok untuk ganjaran!", if (avail) CGreen else Color(0xFF90A4AE), Modifier.fillMaxWidth(), "🎁", fontSize = 20.sp) {
                if (avail) { val c = app.store.claimDaily(p.id); msg = "Dapat 🪙 $c syiling! Hari berturut: ${app.data().streak}"; Audio.play("win") }
            }
            if (msg.isNotEmpty()) Text(msg, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            Text("👕 Kostum", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
            for (c in COSTUMES) {
                val owned = c.id in d.costumes; val on = d.costume == c.id
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                    BigButton(if (on) "Dipakai" else if (owned) "Pakai" else "🪙 ${c.price}", if (on) Color(0xFF90A4AE) else if (owned || d.coins >= c.price) CGreen else CRed, pad = 14.dp, fontSize = 16.sp) {
                        if (!app.store.buyOrEquip(p.id, c)) msg = "Syiling belum cukup. Jom main lagi!"
                    }
                }
            }

            Text("🏅 Lencana (${d.badges.size}/${BADGES.size})", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
            for (row in BADGES.chunked(2)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (b in row) {
                        val got = b.id in d.badges
                        Column(Modifier.weight(1f).alpha(if (got) 1f else 0.4f).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(b.emoji, fontSize = 36.sp)
                            Text(b.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF37474F), textAlign = TextAlign.Center)
                            Text(b.desc, fontSize = 12.sp, color = Color(0xFF607D8B), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Text("📊 Statistik", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(14.dp)) {
                val s = d.stats
                for (l in listOf(
                    "Permainan dimainkan: ${s["games"] ?: 0}", "Tahap pengembaraan: ${d.levels.size}/12",
                    "Syiling dikumpul: ${s["coinsEarned"] ?: 0}", "Larian terbaik: ${s["run_best"] ?: 0}",
                    "Belon terbaik: ${s["balloon_best"] ?: 0}", "Maze tahap: ${s["maze_level"] ?: 0}",
                    "Masa bermain: ${s["minutes"] ?: 0} minit"
                )) Text(l, fontSize = 17.sp, color = Color(0xFF455A64))
            }
        }
    }
}

@Composable
fun SettingsScreen(app: App, onBack: () -> Unit) {
    val t = app.store.tick.intValue
    var ok by remember { mutableStateOf(false) }
    val a = remember { Random.nextInt(3, 10) }; val b = remember { Random.nextInt(3, 10) }
    val options = remember { (listOf(a * b, a * b + 3, a * b - 4, a * b + 10)).shuffled() }
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(BgBrush)) {
        TopBar(app, "Tetapan", onBack)
        if (!ok) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🔒 Untuk ibu bapa", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Spacer(Modifier.height(12.dp))
                Text("Berapakah $a × $b ?", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                for (o in options) { BigButton("$o", CBlue, Modifier.fillMaxWidth(0.6f)) { if (o == a * b) ok = true else Audio.play("lose") }; Spacer(Modifier.height(8.dp)) }
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val st = app.store
                BigButton(if (st.music) "Muzik: HIDUP" else "Muzik: MATI", if (st.music) CGreen else CRed, Modifier.fillMaxWidth(), "🎵") { st.music = !st.music }
                BigButton(if (st.sfx) "Bunyi: HIDUP" else "Bunyi: MATI", if (st.sfx) CGreen else CRed, Modifier.fillMaxWidth(), "🔊") { st.sfx = !st.sfx }
                Text("Kualiti grafik 3D", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((i, n) in listOf("Rendah", "Sederhana", "Tinggi").withIndex())
                        BigButton(n, if (st.quality == i) CGreen else Color(0xFF90A4AE), Modifier.weight(1f), pad = 4.dp, fontSize = 15.sp) { st.quality = i }
                }
                Text("Peringatan rehat (minit)", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in listOf(0, 15, 30, 45, 60))
                        BigButton(if (m == 0) "Off" else "$m", if (st.reminder == m) CGreen else Color(0xFF90A4AE), Modifier.weight(1f), pad = 2.dp, fontSize = 15.sp) { st.reminder = m }
                }
                BigButton("Set semula kemajuan ${app.cur().name}", CRed, Modifier.fillMaxWidth(), "🗑️", fontSize = 16.sp) { confirmReset = true }
                Text("Semua data disimpan di telefon ini sahaja. Tiada iklan, tiada internet, tiada kebenaran khas.", fontSize = 14.sp, color = Color(0xFF455A64))
            }
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Set semula?") },
        text = { Text("Semua kemajuan ${app.cur().name} akan dipadam.") },
        confirmButton = { TextButton(onClick = { app.store.reset(app.cur().id); confirmReset = false }) { Text("Ya, padam") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Batal") } }
    )
}
