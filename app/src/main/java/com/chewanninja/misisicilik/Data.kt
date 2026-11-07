package com.chewanninja.misisicilik

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Color as AColor

class ChildProfile(
    val id: String, var name: String, val skin: Int, val hair: Int, val hairStyle: String,
    val shirt: Int, val pants: Int, val headwear: String, val headwearColor: Int,
    val size: Float, val portrait: ImageBitmap?
)

class Costume(val id: Int, val name: String, val price: Int, val shirt: Int?, val accessory: Int)

val COSTUMES = listOf(
    Costume(0, "Pakaian Biasa", 0, null, 0),
    Costume(1, "Pahlawan Merah", 60, 0xFFD32F2F.toInt(), 1),
    Costume(2, "Angkasawan", 120, 0xFFECEFF1.toInt(), 3),
    Costume(3, "Pengembara Hutan", 90, 0xFF2E7D32.toInt(), 4),
    Costume(4, "Raja Pelangi", 200, 0xFF7E57C2.toInt(), 2)
)

object Characters {
    private val shirts = listOf("#EF5350", "#42A5F5", "#66BB6A", "#FFA726", "#AB47BC")
    private val skins = listOf("#F1C27D", "#E0AC69", "#C68642", "#D9A075", "#FFDBAC")
    private val hairs = listOf("#2B1B14", "#4A2C17", "#111111", "#6D4C41", "#1E1E1E")

    private fun col(s: String?, def: Int): Int = try { AColor.parseColor(s) } catch (e: Exception) { def }

    fun load(ctx: Context): List<ChildProfile> {
        val am = ctx.assets
        val found = (am.list("characters") ?: emptyArray())
            .mapNotNull { Regex("child(\\d+)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .filter { it in 1..20 }.toMutableList()
        var n = 1
        while (found.size < 5) { if (n !in found) found.add(n); n++ }
        found.sort()
        return found.map { make(am, it) }
    }

    private fun make(am: AssetManager, n: Int): ChildProfile {
        val id = "child$n"
        val js = try {
            JSONObject(am.open("characters/$id/profile.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) { JSONObject() }
        var bmp: ImageBitmap? = null
        for (f in listOf("portrait.jpg", "portrait.png", "portrait.webp")) {
            val b = try { am.open("characters/$id/$f").use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
            if (b != null) { bmp = b.asImageBitmap(); break }
        }
        val k = (n - 1) % 5
        return ChildProfile(
            id, js.optString("name", "Kawan $n"),
            col(js.optString("skin", skins[k]), AColor.rgb(230, 180, 140)),
            col(js.optString("hair", hairs[k]), AColor.BLACK),
            js.optString("hairStyle", "short"),
            col(js.optString("shirt", shirts[k]), AColor.RED),
            col(js.optString("pants", "#37474F"), AColor.DKGRAY),
            js.optString("headwear", "none"),
            col(js.optString("headwearColor", "#222222"), AColor.DKGRAY),
            js.optDouble("size", 1.0).toFloat(), bmp
        )
    }
}

class PlayerData {
    var coins = 0; var stars = 0; var xp = 0; var costume = 0
    var lastDaily = ""; var streak = 0
    val levels = HashSet<String>()
    val badges = HashSet<String>()
    val costumes = hashSetOf(0)
    val stats = HashMap<String, Int>()
    val level: Int get() = 1 + xp / 100

    fun toJson(): String {
        val o = JSONObject()
        o.put("coins", coins); o.put("stars", stars); o.put("xp", xp); o.put("costume", costume)
        o.put("lastDaily", lastDaily); o.put("streak", streak)
        o.put("levels", JSONArray(levels.toList())); o.put("badges", JSONArray(badges.toList()))
        o.put("costumes", JSONArray(costumes.toList()))
        val s = JSONObject(); for ((k, v) in stats) s.put(k, v); o.put("stats", s)
        return o.toString()
    }

    companion object {
        fun fromJson(str: String?): PlayerData {
            val d = PlayerData()
            if (str == null) return d
            try {
                val o = JSONObject(str)
                d.coins = o.optInt("coins"); d.stars = o.optInt("stars"); d.xp = o.optInt("xp")
                d.costume = o.optInt("costume"); d.lastDaily = o.optString("lastDaily"); d.streak = o.optInt("streak")
                o.optJSONArray("levels")?.let { a -> for (i in 0 until a.length()) d.levels.add(a.getString(i)) }
                o.optJSONArray("badges")?.let { a -> for (i in 0 until a.length()) d.badges.add(a.getString(i)) }
                o.optJSONArray("costumes")?.let { a -> for (i in 0 until a.length()) d.costumes.add(a.getInt(i)) }
                o.optJSONObject("stats")?.let { s -> for (k in s.keys()) d.stats[k] = s.getInt(k) }
            } catch (e: Exception) { }
            return d
        }
    }
}

class Badge(val id: String, val emoji: String, val title: String, val desc: String, val test: (PlayerData) -> Boolean)

private fun worldDone(d: PlayerData, w: Int) = (0..2).all { "$w-$it" in d.levels }
private fun st(d: PlayerData, k: String) = d.stats[k] ?: 0

val BADGES = listOf(
    Badge("first", "🌱", "Langkah Pertama", "Habiskan 1 tahap") { it.levels.isNotEmpty() },
    Badge("forest", "🌳", "Penjelajah Hutan", "Habiskan Dunia 1") { worldDone(it, 0) },
    Badge("island", "🏝️", "Pelayar Pulau", "Habiskan Dunia 2") { worldDone(it, 1) },
    Badge("city", "🏙️", "Hero Bandar", "Habiskan Dunia 3") { worldDone(it, 2) },
    Badge("castle", "🏰", "Raja Istana", "Habiskan Dunia 4") { worldDone(it, 3) },
    Badge("coins100", "🪙", "Pengumpul Syiling", "Kumpul 100 syiling") { st(it, "coinsEarned") >= 100 },
    Badge("coins500", "💰", "Kaya Raya", "Kumpul 500 syiling") { st(it, "coinsEarned") >= 500 },
    Badge("stars20", "⭐", "Bintang Terang", "Dapat 20 bintang") { it.stars >= 20 },
    Badge("run", "🏃", "Pelari Pantas", "Skor 300 dalam Larian") { st(it, "run_best") >= 300 },
    Badge("balloon", "🎈", "Jaguh Belon", "Skor 400 dalam Belon") { st(it, "balloon_best") >= 400 },
    Badge("memory", "🧠", "Ingatan Kuat", "Menang Memori tahap Susah") { st(it, "memory_hard") >= 1 },
    Badge("maze", "🌀", "Pakar Maze", "Capai Tahap 5 Maze") { st(it, "maze_level") >= 5 },
    Badge("treasure", "🗺️", "Pemburu Harta", "Jumpa 5 harta") { st(it, "treasure_best") >= 5 },
    Badge("daily", "📅", "Setia Hadir", "Ganjaran harian 3 hari") { it.streak >= 3 },
    Badge("lv5", "🚀", "Naik Tahap", "Capai Level 5") { it.level >= 5 },
    Badge("wardrobe", "👑", "Gaya Hebat", "Miliki 3 kostum") { it.costumes.size >= 3 }
)

val WORLD_NAMES = listOf("Hutan Hijau", "Pulau Ajaib", "Bandar Ceria", "Istana Pelangi")

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("misi_si_cilik", Context.MODE_PRIVATE)
    val tick = mutableIntStateOf(0)
    private val cache = HashMap<String, PlayerData>()
    fun bump() { tick.intValue = tick.intValue + 1 }

    var selected: String
        get() = sp.getString("selected", "child1") ?: "child1"
        set(v) { sp.edit().putString("selected", v).apply(); bump() }
    var music: Boolean
        get() = sp.getBoolean("music", true)
        set(v) { sp.edit().putBoolean("music", v).apply(); bump() }
    var sfx: Boolean
        get() = sp.getBoolean("sfx", true)
        set(v) { sp.edit().putBoolean("sfx", v).apply(); bump() }
    var quality: Int
        get() = sp.getInt("quality", 1)
        set(v) { sp.edit().putInt("quality", v).apply(); bump() }
    var reminder: Int
        get() = sp.getInt("reminder", 0)
        set(v) { sp.edit().putInt("reminder", v).apply(); bump() }

    fun nameOf(id: String, def: String) = sp.getString("name_$id", def) ?: def
    fun rename(id: String, n: String) { sp.edit().putString("name_$id", n).apply(); bump() }

    fun data(id: String): PlayerData = cache.getOrPut(id) { PlayerData.fromJson(sp.getString("p_$id", null)) }
    fun save(id: String) { sp.edit().putString("p_$id", data(id).toJson()).apply(); bump() }
    fun reset(id: String) { cache.remove(id); sp.edit().remove("p_$id").apply(); bump() }

    private fun checkBadges(d: PlayerData): List<Badge> {
        val nb = BADGES.filter { it.id !in d.badges && it.test(d) }
        nb.forEach { d.badges.add(it.id) }
        return nb
    }

    fun award(id: String, coins: Int, stars: Int, xp: Int): List<Badge> {
        val d = data(id)
        d.coins += coins; d.stars += stars; d.xp += xp
        d.stats["coinsEarned"] = (d.stats["coinsEarned"] ?: 0) + coins
        d.stats["games"] = (d.stats["games"] ?: 0) + 1
        val nb = checkBadges(d); save(id); return nb
    }

    fun best(id: String, key: String, v: Int) {
        val d = data(id); if (v > (d.stats[key] ?: 0)) { d.stats[key] = v; checkBadges(d); save(id) }
    }

    fun inc(id: String, key: String) { val d = data(id); d.stats[key] = (d.stats[key] ?: 0) + 1; save(id) }

    fun completeLevel(id: String, key: String, stars: Int): Int {
        val d = data(id); d.levels.add(key)
        val prev = d.stats["lv_$key"] ?: 0
        if (stars > prev) d.stats["lv_$key"] = stars
        return maxOf(0, stars - prev)
    }

    private val fmt get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun today(): String = fmt.format(Date())
    private fun yesterday(): String { val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -1); return fmt.format(c.time) }
    fun dailyAvailable(id: String) = data(id).lastDaily != today()
    fun claimDaily(id: String): Int {
        val d = data(id)
        d.streak = if (d.lastDaily == yesterday()) d.streak + 1 else 1
        d.lastDaily = today()
        val c = 10 + minOf(d.streak, 7) * 5
        d.coins += c
        if (d.streak % 7 == 0) d.stars += 1
        checkBadges(d); save(id); return c
    }

    fun buyOrEquip(id: String, c: Costume): Boolean {
        val d = data(id)
        if (c.id !in d.costumes) { if (d.coins < c.price) return false; d.coins -= c.price; d.costumes.add(c.id) }
        d.costume = c.id; checkBadges(d); save(id); return true
    }
}
