package com.chewanninja.misisicilik

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Theme(val sky: FloatArray, val ground: Int, val top: Int, val foe: Int, val pit: Int?, val back: Int?)

val THEMES = listOf(
    Theme(floatArrayOf(0.53f, 0.81f, 0.95f), 0xFF8D6E63.toInt(), 0xFF66BB6A.toInt(), 0xFF7CB342.toInt(), 0xFF29B6F6.toInt(), 0xFF43A047.toInt()),
    Theme(floatArrayOf(0.72f, 0.62f, 0.98f), 0xFF9575CD.toInt(), 0xFFF48FB1.toInt(), 0xFFF06292.toInt(), null, null),
    Theme(floatArrayOf(1f, 0.80f, 0.55f), 0xFF607D8B.toInt(), 0xFF455A64.toInt(), 0xFFFF9800.toInt(), 0xFF37474F.toInt(), 0xFF78909C.toInt()),
    Theme(floatArrayOf(0.55f, 0.45f, 0.85f), 0xFFBDBDBD.toInt(), 0xFFCE93D8.toInt(), 0xFFFFFFFF.toInt(), 0xFF4A148C.toInt(), 0xFF7E57C2.toInt())
)

class Plat(
    val cx0: Float, val top0: Float, val w: Float, val h: Float, val color: Int, val topColor: Int,
    val moving: Boolean = false, val amp: Float = 0f, val spd: Float = 0f
) {
    var cx = cx0; var top = top0; var dx = 0f; var dy = 0f
    fun upd(t: Float) {
        if (!moving) return
        val nx = cx0 + sin(t * spd) * amp
        dx = nx - cx; cx = nx
    }
}

class Item(val x: Float, val y: Float, val kind: Int) { var got = false }          // 0 coin, 1 star, 2 chest
class Foe(val bx: Float, val y: Float, val amp: Float, val spd: Float, val ph: Float) { var alive = true; var x = bx }
class Deco(val kind: Int, val x: Float, val y: Float, val z: Float, val s: Float, val c: Int, val minQ: Int)
class Level(
    val plats: List<Plat>, val items: List<Item>, val foes: List<Foe>, val decos: List<Deco>,
    val goalX: Float, val goalY: Float, val theme: Theme, val length: Float
) { val totalCoins = items.count { it.kind == 0 } }

object LevelGen {
    private val crystal = intArrayOf(0xFF4DD0E1.toInt(), 0xFFFF80AB.toInt(), 0xFFB388FF.toInt(), 0xFFFFF59D.toInt())
    private val town = intArrayOf(0xFFEF5350.toInt(), 0xFF42A5F5.toInt(), 0xFFFFCA28.toInt(), 0xFF26A69A.toInt(), 0xFFAB47BC.toInt())

    fun build(world: Int, idx: Int): Level {
        val th = THEMES[world]
        val rnd = Random(world * 1000 + idx * 77 + 5)
        val plats = ArrayList<Plat>(); val items = ArrayList<Item>()
        val foes = ArrayList<Foe>(); val decos = ArrayList<Deco>()
        val wood = 0xFF8D6E63.toInt(); val woodTop = 0xFFBCAAA4.toInt()
        plats.add(Plat(3f, 0f, 10f, 12f, th.ground, th.top))
        var right = 8f; var top = 0f
        val chunks = 10 + idx * 4 + world * 3
        val starChunks = HashSet<Int>()
        while (starChunks.size < 3) starChunks.add(2 + rnd.nextInt(chunks - 2))
        val chestChunk = 3 + rnd.nextInt(chunks - 4)
        val steps = floatArrayOf(0f, 0f, 0.9f, 1.2f, -0.9f, -1.2f)
        for (k in 0 until chunks) {
            var gap = 1.8f + rnd.nextFloat() * 1.5f + world * 0.15f
            var ntop = (top + steps[rnd.nextInt(steps.size)]).coerceIn(0f, 3f)
            if (ntop > top + 0.5f) gap = min(gap, 2.6f)
            val bridge = (world == 0 || world == 3) && gap > 2.2f && rnd.nextInt(100) < 45
            if (bridge) ntop = top
            val mover = (world == 1 || world == 2) && rnd.nextInt(100) < 50
            if (mover) { gap = 3.4f; if (ntop > top + 0.5f) ntop = top }
            if (bridge) plats.add(Plat(right + gap / 2f, top, gap + 0.6f, 0.35f, wood, woodTop))
            if (mover) plats.add(Plat(right + gap / 2f, (top + ntop) / 2f, 2.2f, 0.4f, 0xFFFFB74D.toInt(), 0xFFFFE082.toInt(), true, 0.7f, 1.5f))
            val len = 5f + rnd.nextFloat() * 4f
            val left = right + gap; val cx = left + len / 2f
            plats.add(Plat(cx, ntop, len, 12f, th.ground, th.top))
            val n = (len / 1.3f).toInt()
            for (i in 0 until n) {
                val x = left + 0.9f + i * (len - 1.8f) / max(1, n - 1)
                items.add(Item(x, ntop + 0.7f + 0.4f * abs(sin(i * 0.8f)), 0))
            }
            if (len >= 6.5f && k >= 1 && rnd.nextInt(100) < 35 + world * 10 + idx * 5)
                foes.add(Foe(cx, ntop, 1.4f, 1.3f + rnd.nextFloat() * 0.6f, rnd.nextFloat() * 6f))
            if (k in starChunks || k == chestChunk || rnd.nextInt(100) < 25) {
                val fy = ntop + 1.5f; val fx = cx + (rnd.nextFloat() - 0.5f) * 1.5f
                plats.add(Plat(fx, fy, 2.4f, 0.35f, wood, 0xFFFFD54F.toInt()))
                if (k in starChunks) items.add(Item(fx, fy + 0.8f, 1))
                else if (k == chestChunk) items.add(Item(fx, fy, 2))
                else { items.add(Item(fx - 0.5f, fy + 0.7f, 0)); items.add(Item(fx + 0.5f, fy + 0.7f, 0)) }
            }
            val dx = left + rnd.nextFloat() * len
            when (world) {
                0 -> {
                    decos.add(Deco(0, dx, 0f, -3f - rnd.nextFloat() * 3f, 1.2f + rnd.nextFloat(), 0, 0))
                    decos.add(Deco(1, left + rnd.nextFloat() * len, ntop, -1f, 0.8f, 0, 1))
                }
                1 -> {
                    decos.add(Deco(2, dx, ntop, -1f, 0.8f + rnd.nextFloat() * 0.8f, crystal[rnd.nextInt(4)], 0))
                    decos.add(Deco(2, dx + 1f, ntop, -1.2f, 0.6f, crystal[rnd.nextInt(4)], 1))
                }
                2 -> decos.add(Deco(3, dx, 0f, -4.5f - rnd.nextFloat() * 3f, 4f + rnd.nextFloat() * 6f, town[rnd.nextInt(5)], 0))
                else -> decos.add(Deco(4, dx, 0f, -4.5f - rnd.nextFloat() * 2f, 5f + rnd.nextFloat() * 4f, 0xFF9E9E9E.toInt(), 0))
            }
            if (k % 2 == 0) decos.add(Deco(5, left, 5f + rnd.nextFloat() * 3f, -9f, 1f + rnd.nextFloat(), 0, 1))
            right = left + len; top = ntop
        }
        val left = right + 2.5f
        plats.add(Plat(left + 5f, top, 10f, 12f, th.ground, th.top))
        items.add(Item(left + 3.2f, top, 2))
        return Level(plats, items, foes, decos, left + 6.5f, top, th, right + 30f)
    }
}

data class Hud(val coins: Int, val stars: Int, val hearts: Int, val state: Int, val prog: Float, val total: Int)

class AdventureScene(
    val p: ChildProfile, val costume: Costume, val world: Int, val idx: Int, val quality: Int
) : Scene {
    private val lvl = LevelGen.build(world, idx)
    override val sky = lvl.theme.sky
    @Volatile var left = false; @Volatile var right = false; @Volatile var run = false
    @Volatile var jumpReq = false; @Volatile var paused = false
    @Volatile var state = 0
    @Volatile var hearts = 5; @Volatile var coins = 0; @Volatile var stars = 0

    private val HW = 0.25f; private val H = 1.0f
    private var px = 0f; private var py = 0f; private var vx = 0f; private var vy = 0f
    private var facing = 1f; private var onGround = true; private var coyote = 0f
    private var riding: Plat? = null
    private var invul = 0f; private var t = 0f; private var dtLast = 0.016f
    private var safeX = 0f; private var safeY = 0f
    private var camX = 2.5f; private var camY = 0f

    fun hud() = Hud(coins, stars, hearts, state, (px / lvl.goalX).coerceIn(0f, 1f), lvl.totalCoins)

    private fun hurt(fall: Boolean) {
        hearts -= 1
        Audio.play("hit")
        if (hearts <= 0) { state = 2; Audio.play("lose"); return }
        invul = 1.6f
        if (fall) { px = safeX; py = safeY + 0.05f; vx = 0f; vy = 0f }
        else { vy = 7.5f; vx = -facing * 6f }
    }

    override fun update(dt: Float) {
        if (paused) return
        dtLast = dt; t += dt
        for (pl in lvl.plats) pl.upd(t)
        for (f in lvl.foes) if (f.alive) f.x = f.bx + sin(t * f.spd + f.ph) * f.amp
        if (state != 0) return
        invul = max(0f, invul - dt)
        val dir = (if (right) 1 else 0) - (if (left) 1 else 0)
        val target = dir * (if (run) 8.5f else 5.5f)
        val acc = 45f * dt
        vx = if (vx < target) min(target, vx + acc) else max(target, vx - acc)
        if (dir != 0) facing = dir.toFloat()
        riding?.let { px += it.dx }
        coyote -= dt
        if (onGround) coyote = 0.12f
        if (jumpReq) {
            if (coyote > 0f) { vy = 11.8f; onGround = false; coyote = 0f; Audio.play("jump") }
            jumpReq = false
        }
        vy = max(-25f, vy - 30f * dt)
        // horizontal move + side collisions with solid platforms
        val ox = px
        px += vx * dt
        if (px < -1.5f) { px = -1.5f; vx = 0f }
        for (pl in lvl.plats) {
            if (pl.moving) continue
            val l = pl.cx - pl.w / 2f; val r = pl.cx + pl.w / 2f; val bottom = pl.top - pl.h
            if (px + HW > l && px - HW < r && py < pl.top - 0.05f && py + H > bottom) {
                px = if (ox < pl.cx) l - HW else r + HW
                vx = 0f
            }
        }
        // vertical move + landings
        val oy = py
        py += vy * dt
        onGround = false; riding = null
        for (pl in lvl.plats) {
            val l = pl.cx - pl.w / 2f; val r = pl.cx + pl.w / 2f
            if (px + HW <= l || px - HW >= r) continue
            if (pl.moving) {
                if (vy <= 0f && oy >= pl.top - 0.08f && py <= pl.top) { py = pl.top; vy = 0f; onGround = true; riding = pl }
            } else {
                if (vy <= 0f && oy >= pl.top - 0.08f && py <= pl.top) {
                    py = pl.top; vy = 0f; onGround = true
                    safeX = l + 1.2f; safeY = pl.top
                } else if (vy > 0f && oy + H <= pl.top - pl.h + 0.08f && py + H > pl.top - pl.h) {
                    py = pl.top - pl.h - H; vy = 0f
                }
            }
        }
        if (py < -4f) hurt(true)
        if (state != 0) return
        for (f in lvl.foes) {
            if (!f.alive) continue
            if (abs(px - f.x) < 0.65f && py < f.y + 0.8f && py + H > f.y) {
                if (vy < 0f && py > f.y + 0.4f) { f.alive = false; vy = 9.5f; coins += 1; Audio.play("coin") }
                else if (invul <= 0f) hurt(false)
            }
        }
        for (it in lvl.items) {
            if (it.got) continue
            val dx = px - it.x; val dy = py + 0.5f - (it.y + if (it.kind == 2) 0.3f else 0f)
            val rr = if (it.kind == 0) 0.8f else 1.0f
            if (dx * dx + dy * dy < rr * rr) {
                it.got = true
                when (it.kind) {
                    0 -> { coins += 1; Audio.play("coin") }
                    1 -> { stars += 1; Audio.play("star") }
                    else -> { coins += 15; Audio.play("win") }
                }
            }
        }
        if (px >= lvl.goalX) { state = 1; Audio.play("win") }
    }

    private fun deco(r: Renderer, d: Deco) {
        val s = d.s
        when (d.kind) {
            0 -> {
                r.box(d.x, d.y + s * 0.6f, d.z, 0.35f * s, 1.2f * s, 0.35f * s, 0xFF6D4C41.toInt())
                r.sphere(d.x, d.y + s * 1.7f, d.z, 1.7f * s, 1.5f * s, 1.7f * s, 0xFF2E7D32.toInt())
                r.sphere(d.x, d.y + s * 2.4f, d.z, 1.1f * s, 1.0f * s, 1.1f * s, 0xFF66BB6A.toInt())
            }
            1 -> r.sphere(d.x, d.y + 0.3f * s, d.z, 1.1f * s, 0.7f * s, 0.9f * s, 0xFF9E9E9E.toInt())
            2 -> {
                r.box(d.x, d.y + 0.9f * s, d.z, 0.5f * s, 1.8f * s, 0.5f * s, d.c, 45f)
                r.box(d.x + 0.35f * s, d.y + 0.5f * s, d.z, 0.3f * s, 1.0f * s, 0.3f * s, d.c, 20f)
            }
            3 -> {
                r.box(d.x, d.y + s / 2f, d.z, 2.6f, s, 2.2f, d.c)
                r.box(d.x, d.y + s + 0.15f, d.z, 2.8f, 0.3f, 2.4f, 0xFF37474F.toInt())
                if (quality >= 2) {
                    var wy = 1f
                    while (wy < s - 0.5f) { r.box(d.x - 0.6f, d.y + wy, d.z + 1.12f, 0.5f, 0.5f, 0.05f, 0xFFFFF59D.toInt()); r.box(d.x + 0.6f, d.y + wy, d.z + 1.12f, 0.5f, 0.5f, 0.05f, 0xFFFFF59D.toInt()); wy += 1.4f }
                }
            }
            4 -> {
                r.box(d.x, d.y + s / 2f, d.z, 2.2f, s, 2.2f, d.c)
                r.box(d.x, d.y + s + 0.3f, d.z, 2.6f, 0.6f, 2.6f, 0xFF757575.toInt())
                r.box(d.x, d.y + s + 1.1f, d.z, 1.2f, 1.0f, 1.2f, 0xFFE91E63.toInt(), 45f)
                r.box(d.x, d.y + s * 0.6f, d.z + 1.12f, 0.6f, 1.0f, 0.05f, 0xFF4A148C.toInt())
            }
            else -> {
                r.sphere(d.x, d.y, d.z, 3f * s, 1.3f * s, 1.3f * s, 0xFFFFFFFF.toInt())
                r.sphere(d.x + 1f * s, d.y + 0.3f * s, d.z, 2f * s, 1.2f * s, 1.2f * s, 0xFFFFFFFF.toInt())
            }
        }
    }

    override fun draw(r: Renderer) {
        val k = min(1f, dtLast * 4f)
        camX += (px + 2.5f - camX) * k
        camY += (max(py, 0f) * 0.6f - camY) * k
        r.setCamera(camX, camY + 3.2f, 11.5f, camX, camY + 1f, 0f)
        val dist = when (quality) { 0 -> 15f; 1 -> 22f; else -> 30f }
        val th = lvl.theme
        th.back?.let { r.box(lvl.length / 2f, -3f, -6.5f, lvl.length + 60f, 6f, 9f, it) }
        th.pit?.let { r.box(lvl.length / 2f, -3.2f, 0f, lvl.length + 60f, 2.4f, 12f, it) }
        for (pl in lvl.plats) {
            if (abs(pl.cx - camX) - pl.w / 2f > dist) continue
            r.box(pl.cx, pl.top - pl.h / 2f, 0f, pl.w, pl.h, 3f, pl.color)
            r.box(pl.cx, pl.top - 0.12f, 0f, pl.w + 0.02f, 0.24f, 3.04f, pl.topColor)
        }
        if (quality >= 1 || world == 1) {
            for (d in lvl.decos) {
                if (d.minQ > quality || abs(d.x - camX) > dist + 8f) continue
                deco(r, d)
            }
        }
        for (it in lvl.items) {
            if (abs(it.x - camX) > dist) continue
            when (it.kind) {
                0 -> if (!it.got) r.sphere(it.x, it.y, 0f, 0.55f, 0.55f, 0.14f, GOLD, t * 180f)
                1 -> if (!it.got) {
                    r.box(it.x, it.y, 0f, 0.55f, 0.55f, 0.55f, 0xFFFFEB3B.toInt(), t * 120f)
                    r.box(it.x, it.y, 0f, 0.55f, 0.55f, 0.55f, GOLD, t * 120f + 45f)
                }
                else -> {
                    r.box(it.x, it.y + 0.25f, 0f, 0.9f, 0.5f, 0.6f, 0xFF8D6E63.toInt())
                    r.box(it.x, it.y + 0.6f + (if (it.got) 0.25f else 0f), 0f, 0.92f, 0.25f, 0.62f, 0xFFFFB300.toInt())
                    r.box(it.x, it.y + 0.45f, 0.32f, 0.16f, 0.2f, 0.06f, GOLD)
                }
            }
        }
        for (f in lvl.foes) {
            if (!f.alive || abs(f.x - camX) > dist) continue
            val sq = sin(t * 6f + f.ph) * 0.05f
            r.sphere(f.x, f.y + 0.42f, 0f, 0.9f - sq, 0.8f + sq, 0.9f - sq, lvl.theme.foe)
            r.sphere(f.x - 0.2f, f.y + 0.55f, 0.38f, 0.22f, 0.22f, 0.1f, 0xFFFFFFFF.toInt())
            r.sphere(f.x + 0.2f, f.y + 0.55f, 0.38f, 0.22f, 0.22f, 0.1f, 0xFFFFFFFF.toInt())
            r.sphere(f.x - 0.2f, f.y + 0.55f, 0.44f, 0.1f, 0.1f, 0.06f, 0xFF212121.toInt())
            r.sphere(f.x + 0.2f, f.y + 0.55f, 0.44f, 0.1f, 0.1f, 0.06f, 0xFF212121.toInt())
        }
        r.box(lvl.goalX, lvl.goalY + 1.5f, 0f, 0.12f, 3f, 0.12f, 0xFFFFFFFF.toInt())
        r.box(lvl.goalX + 0.5f + sin(t * 4f) * 0.08f, lvl.goalY + 2.7f, 0f, 1f, 0.6f, 0.06f, 0xFFE53935.toInt())
        if (invul <= 0f || ((t * 12f).toInt() % 2 == 0)) {
            Kid3D.draw(r, p, costume, px, py, 0f, facing * 55f, t, abs(vx) > 0.5f && onGround, !onGround, 0.72f)
        }
    }
}
