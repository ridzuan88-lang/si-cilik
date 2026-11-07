package com.chewanninja.misisicilik

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

interface Scene {
    val sky: FloatArray
    fun update(dt: Float)
    fun draw(r: Renderer)
}

class GLView(ctx: Context) : GLSurfaceView(ctx) {
    val renderer = Renderer()
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

/** Tiny OpenGL ES 2.0 toon renderer: unit cube + unit sphere meshes, per-draw colour, 3-step cel shading. */
class Renderer : GLSurfaceView.Renderer {
    @Volatile var scene: Scene? = null
    private var prog = 0
    private var aPos = 0; private var aNor = 0
    private var uMVP = 0; private var uModel = 0; private var uColor = 0; private var uLight = 0
    private val proj = FloatArray(16); private val view = FloatArray(16); private val vp = FloatArray(16)
    private val model = FloatArray(16); private val mvp = FloatArray(16)
    private var last = 0L
    private lateinit var cube: FloatBuffer
    private lateinit var sph: FloatBuffer
    private var sphCount = 0

    private val vs = """
        uniform mat4 uMVP; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNor; varying vec3 vN;
        void main(){ vN=(uModel*vec4(aNor,0.0)).xyz; gl_Position=uMVP*vec4(aPos,1.0); }
    """.trimIndent()
    private val fs = """
        precision mediump float; varying vec3 vN; uniform vec4 uColor; uniform vec3 uLight;
        void main(){ float d=max(dot(normalize(vN),uLight),0.0); float s=floor(d*3.0+0.5)/3.0;
        float l=0.5+0.5*s; gl_FragColor=vec4(uColor.rgb*l,1.0); }
    """.trimIndent()

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
        return s
    }

    private fun buf(a: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().also { it.put(a); it.position(0) }

    private fun cubeData(): FloatArray {
        val faces = arrayOf(
            floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f),
            floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f),
            floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f),
            floatArrayOf(-1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f),
            floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, -1f),
            floatArrayOf(0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
        )
        val corners = arrayOf(intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(1, 1), intArrayOf(-1, -1), intArrayOf(1, 1), intArrayOf(-1, 1))
        val out = ArrayList<Float>()
        for (f in faces) for (c in corners) {
            for (k in 0..2) out.add(f[k] * 0.5f + (c[0] * f[3 + k] + c[1] * f[6 + k]) * 0.5f)
            for (k in 0..2) out.add(f[k])
        }
        return out.toFloatArray()
    }

    private fun sphereData(): FloatArray {
        val st = 8; val sl = 12
        fun pt(i: Int, j: Int, o: ArrayList<Float>) {
            val th = Math.PI * i / st; val ph = 2.0 * Math.PI * j / sl
            val x = (sin(th) * cos(ph)).toFloat(); val y = cos(th).toFloat(); val z = (sin(th) * sin(ph)).toFloat()
            o.add(x * 0.5f); o.add(y * 0.5f); o.add(z * 0.5f); o.add(x); o.add(y); o.add(z)
        }
        val out = ArrayList<Float>()
        for (i in 0 until st) for (j in 0 until sl) {
            pt(i, j, out); pt(i + 1, j, out); pt(i + 1, j + 1, out)
            pt(i, j, out); pt(i + 1, j + 1, out); pt(i, j + 1, out)
        }
        sphCount = out.size / 6
        return out.toFloatArray()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, compile(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(prog, compile(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(prog)
        aPos = GLES20.glGetAttribLocation(prog, "aPos"); aNor = GLES20.glGetAttribLocation(prog, "aNor")
        uMVP = GLES20.glGetUniformLocation(prog, "uMVP"); uModel = GLES20.glGetUniformLocation(prog, "uModel")
        uColor = GLES20.glGetUniformLocation(prog, "uColor"); uLight = GLES20.glGetUniformLocation(prog, "uLight")
        cube = buf(cubeData()); sph = buf(sphereData())
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        last = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        Matrix.perspectiveM(proj, 0, 42f, w / h.toFloat(), 0.1f, 90f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = min(0.05f, (now - last) / 1e9f); last = now
        val s = scene
        if (s == null) { GLES20.glClearColor(0.5f, 0.8f, 1f, 1f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT); return }
        s.update(dt)
        GLES20.glClearColor(s.sky[0], s.sky[1], s.sky[2], 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(prog)
        GLES20.glEnableVertexAttribArray(aPos); GLES20.glEnableVertexAttribArray(aNor)
        GLES20.glUniform3f(uLight, 0.42f, 0.80f, 0.43f)
        s.draw(this)
    }

    fun setCamera(ex: Float, ey: Float, ez: Float, tx: Float, ty: Float, tz: Float) {
        Matrix.setLookAtM(view, 0, ex, ey, ez, tx, ty, tz, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    private fun mesh(b: FloatBuffer, count: Int, color: Int) {
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform4f(uColor, Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, 1f)
        b.position(0); GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, b)
        b.position(3); GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 24, b)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
    }

    private fun place(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, rotY: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
        Matrix.scaleM(model, 0, w, h, d)
    }

    fun box(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, color: Int, rotY: Float = 0f) {
        place(x, y, z, w, h, d, rotY); mesh(cube, 36, color)
    }

    fun sphere(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, color: Int, rotY: Float = 0f) {
        place(x, y, z, w, h, d, rotY); mesh(sph, sphCount, color)
    }
}
