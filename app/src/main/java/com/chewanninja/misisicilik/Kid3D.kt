package com.chewanninja.misisicilik

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val GOLD = 0xFFFFC107.toInt()
private val INK = 0xFF222222.toInt()

/** Procedural cartoon child built from boxes and spheres, coloured from the child's profile.json. */
object Kid3D {
    fun draw(
        r: Renderer, p: ChildProfile, c: Costume, x: Float, y: Float, z: Float, yaw: Float,
        t: Float, moving: Boolean, air: Boolean, scale: Float
    ) {
        val s = scale * p.size
        val rad = Math.toRadians(yaw.toDouble())
        val cs = cos(rad).toFloat(); val sn = sin(rad).toFloat()
        fun b(lx: Float, ly: Float, lz: Float, w: Float, h: Float, d: Float, col: Int) =
            r.box(x + (lx * cs + lz * sn) * s, y + ly * s, z + (-lx * sn + lz * cs) * s, w * s, h * s, d * s, col, yaw)
        fun o(lx: Float, ly: Float, lz: Float, w: Float, h: Float, d: Float, col: Int) =
            r.sphere(x + (lx * cs + lz * sn) * s, y + ly * s, z + (-lx * sn + lz * cs) * s, w * s, h * s, d * s, col, yaw)

        val sw = if (moving) sin(t * 13f) else 0f
        val bob = if (moving) abs(sin(t * 13f)) * 0.05f else 0f
        val up = if (air) 0.28f else 0f
        val shirt = c.shirt ?: p.shirt
        val hijab = p.headwear.startsWith("hijab")
        val cap = p.headwear.endsWith("cap")
        val capCol = if (p.headwear == "cap") p.headwearColor else INK

        // legs + shoes
        b(-0.14f, 0.2f, sw * 0.18f, 0.2f, 0.4f, 0.22f, p.pants)
        b(0.14f, 0.2f, -sw * 0.18f, 0.2f, 0.4f, 0.22f, p.pants)
        b(-0.14f, 0.05f, sw * 0.18f + 0.04f, 0.22f, 0.1f, 0.3f, 0xFFEEEEEE.toInt())
        b(0.14f, 0.05f, -sw * 0.18f + 0.04f, 0.22f, 0.1f, 0.3f, 0xFFEEEEEE.toInt())
        // body + arms
        b(0f, 0.72f + bob, 0f, 0.54f, 0.55f, 0.36f, shirt)
        b(-0.36f, 0.74f + bob + up, -sw * 0.12f, 0.15f, 0.48f, 0.15f, shirt)
        b(0.36f, 0.74f + bob + up, sw * 0.12f, 0.15f, 0.48f, 0.15f, shirt)
        o(-0.36f, 0.47f + bob + up, -sw * 0.12f, 0.17f, 0.17f, 0.17f, p.skin)
        o(0.36f, 0.47f + bob + up, sw * 0.12f, 0.17f, 0.17f, 0.17f, p.skin)
        // head
        o(0f, 1.2f + bob, 0f, 0.8f, 0.76f, 0.76f, p.skin)
        o(-0.15f, 1.23f + bob, 0.34f, 0.1f, 0.12f, 0.06f, INK)
        o(0.15f, 1.23f + bob, 0.34f, 0.1f, 0.12f, 0.06f, INK)
        o(-0.25f, 1.12f + bob, 0.32f, 0.1f, 0.07f, 0.04f, 0xFFFF9E9E.toInt())
        o(0.25f, 1.12f + bob, 0.32f, 0.1f, 0.07f, 0.04f, 0xFFFF9E9E.toInt())
        b(0f, 1.06f + bob, 0.37f, 0.14f, 0.03f, 0.03f, 0xFF8D3B3B.toInt())
        // hair / headwear
        if (hijab) {
            o(0f, 1.22f + bob, -0.16f, 0.98f, 0.98f, 0.86f, p.headwearColor)
            b(0f, 0.98f + bob, -0.02f, 0.74f, 0.42f, 0.5f, p.headwearColor)
        } else if (p.hairStyle != "none") {
            o(0f, 1.36f + bob, -0.06f, 0.9f, 0.6f, 0.88f, p.hair)
            if (p.hairStyle == "bangs") b(0f, 1.47f + bob, 0.3f, 0.66f, 0.12f, 0.14f, p.hair)
            if (p.hairStyle == "long") b(0f, 1.0f + bob, -0.28f, 0.78f, 0.8f, 0.2f, p.hair)
            if (p.hairStyle == "spiky") for (i in -1..1) b(i * 0.22f, 1.72f + bob, 0f, 0.14f, 0.26f, 0.14f, p.hair)
        }
        if (cap) {
            o(0f, 1.52f + bob, -0.02f, 0.88f, 0.42f, 0.88f, capCol)
            b(0f, 1.42f + bob, 0.38f, 0.62f, 0.05f, 0.36f, capCol)
        }
        // costume accessories
        when (c.accessory) {
            1 -> b(0f, 0.7f + bob, -0.26f, 0.6f, 0.8f, 0.06f, 0xFF1565C0.toInt())
            2 -> for (i in -1..1) b(i * 0.2f, 1.72f + bob, 0f, 0.14f, 0.22f + (1 - abs(i)) * 0.1f, 0.14f, GOLD)
            3 -> { b(0f, 1.72f + bob, 0f, 0.04f, 0.22f, 0.04f, 0xFF9E9E9E.toInt()); o(0f, 1.86f + bob, 0f, 0.13f, 0.13f, 0.13f, 0xFFE53935.toInt()) }
            4 -> o(0f, 1.64f + bob, -0.02f, 0.62f, 0.2f, 0.62f, 0xFF43A047.toInt())
        }
    }
}

class MenuScene(var profile: ChildProfile, var costume: Costume) : Scene {
    override val sky = floatArrayOf(0.45f, 0.78f, 1f)
    private var t = 0f
    override fun update(dt: Float) { t += dt }
    override fun draw(r: Renderer) {
        r.setCamera(0f, 1.9f, 8.5f, 0f, 1.1f, 0f)
        r.sphere(-4f, -4.2f, -7f, 12f, 8f, 8f, 0xFF81C784.toInt())
        r.sphere(5f, -4.6f, -9f, 14f, 8f, 8f, 0xFF66BB6A.toInt())
        for (i in 0 until 4) {
            val cx = ((t * 0.4f + i * 3.1f) % 12f) - 6f
            val cy = 4f + i * 0.5f
            r.sphere(cx, cy, -6f, 1.8f, 0.9f, 0.9f, 0xFFFFFFFF.toInt())
            r.sphere(cx + 0.7f, cy + 0.2f, -6f, 1.2f, 0.9f, 0.9f, 0xFFFFFFFF.toInt())
        }
        r.box(0f, -0.2f, 0f, 2.8f, 0.4f, 2.8f, 0xFF8D6E63.toInt())
        r.box(0f, 0.02f, 0f, 2.9f, 0.12f, 2.9f, 0xFF66BB6A.toInt())
        val hop = abs(sin(t * 2.4f)) * 0.25f
        Kid3D.draw(r, profile, costume, 0f, 0.08f + hop, 0f, sin(t * 0.9f) * 40f, t, false, hop > 0.05f, 1.35f)
        for (i in 0 until 5) {
            val a = t * 1.2f + i * 1.2566f
            r.box(cos(a) * 1.5f, 1.5f + sin(t * 2f + i) * 0.35f, sin(a) * 0.9f, 0.28f, 0.28f, 0.28f, GOLD, t * 90f)
        }
    }
}
