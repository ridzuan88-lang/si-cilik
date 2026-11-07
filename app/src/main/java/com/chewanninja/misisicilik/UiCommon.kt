package com.chewanninja.misisicilik

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgBrush = Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE)))
val CGreen = Color(0xFF43A047); val COrange = Color(0xFFFF9800); val CBlue = Color(0xFF29B6F6)
val CPink = Color(0xFFEC407A); val CPurple = Color(0xFF8E24AA); val CRed = Color(0xFFEF5350)

@Composable
fun BigButton(
    text: String, color: Color, modifier: Modifier = Modifier, icon: String = "", pad: Dp = 20.dp,
    fontSize: TextUnit = 22.sp, onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.92f else 1f, label = "btn")
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .shadow(6.dp, shape)
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color)), shape)
            .clickable(interactionSource = src, indication = null) { Audio.play("click"); onClick() }
            .padding(horizontal = pad, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon.isNotEmpty()) Text(icon, fontSize = (fontSize.value + 6).sp)
            if (icon.isNotEmpty() && text.isNotEmpty()) Spacer(Modifier.width(8.dp))
            if (text.isNotEmpty()) Text(text, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Chip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
    }
}

@Composable
fun TopBar(app: App, title: String, onBack: () -> Unit) {
    val t = app.store.tick.intValue
    val d = app.data()
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BigButton("", COrange, Modifier.size(56.dp), "⬅", pad = 0.dp) { onBack() }
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B), textAlign = TextAlign.Center)
        Chip("🪙 ${d.coins}"); Spacer(Modifier.width(6.dp)); Chip("⭐ ${d.stars}")
    }
}

private val textPaint = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
fun DrawScope.label(s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT) {
    textPaint.color = color; textPaint.textSize = size; textPaint.textAlign = align
    drawContext.canvas.nativeCanvas.drawText(s, x, y, textPaint)
}

/** 2D cartoon kid (used for portraits fallback and the 2D mini-games). cx,cy = head centre, s = head radius. */
fun DrawScope.drawKid(p: ChildProfile, cx: Float, cy: Float, s: Float, shirtOverride: Int? = null) {
    val skin = Color(p.skin); val hair = Color(p.hair)
    val hijab = p.headwear.startsWith("hijab"); val cap = p.headwear.endsWith("cap")
    val hw = Color(p.headwearColor)
    drawRoundRect(Color(shirtOverride ?: p.shirt), Offset(cx - s * 0.9f, cy + s * 0.85f), Size(s * 1.8f, s * 1.5f), CornerRadius(s * 0.5f))
    if (hijab) {
        drawCircle(hw, s * 1.18f, Offset(cx, cy))
        drawRoundRect(hw, Offset(cx - s * 1.05f, cy + s * 0.6f), Size(s * 2.1f, s * 1.3f), CornerRadius(s * 0.6f))
    } else if (p.hairStyle == "long") {
        drawRoundRect(hair, Offset(cx - s * 1.05f, cy - s * 0.2f), Size(s * 2.1f, s * 1.8f), CornerRadius(s * 0.7f))
    }
    drawCircle(skin, s, Offset(cx, cy))
    if (!hijab && p.hairStyle != "none") {
        drawArc(hair, 180f, 180f, true, Offset(cx - s * 1.05f, cy - s * 1.05f), Size(s * 2.1f, s * 1.5f))
        if (p.hairStyle == "spiky") for (i in -1..1) drawCircle(hair, s * 0.22f, Offset(cx + i * s * 0.5f, cy - s * 1.1f))
    }
    if (cap) {
        val cc = if (p.headwear == "cap") hw else Color(0xFF222222)
        drawArc(cc, 180f, 180f, true, Offset(cx - s * 1.05f, cy - s * 1.15f), Size(s * 2.1f, s * 1.3f))
        drawRoundRect(cc, Offset(cx - s * 0.2f, cy - s * 0.35f), Size(s * 1.3f, s * 0.2f), CornerRadius(s * 0.1f))
    }
    drawCircle(Color(0xFF222222), s * 0.11f, Offset(cx - s * 0.35f, cy - s * 0.02f))
    drawCircle(Color(0xFF222222), s * 0.11f, Offset(cx + s * 0.35f, cy - s * 0.02f))
    drawArc(Color(0xFF8D3B3B), 20f, 140f, false, Offset(cx - s * 0.3f, cy + s * 0.1f), Size(s * 0.6f, s * 0.4f), style = Stroke(s * 0.07f))
}

@Composable
fun Portrait(p: ChildProfile, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(Color(0xFFFFF3E0)).border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (p.portrait != null) Image(p.portrait, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Canvas(Modifier.fillMaxSize()) { drawKid(p, this.size.width / 2f, this.size.height * 0.42f, this.size.minDimension * 0.27f) }
    }
}

class Outcome(val title: String, val lines: List<String>, val badges: List<Badge>, val win: Boolean = true)

@Composable
fun ResultCard(o: Outcome, primary: String, onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit) {
    LaunchedEffect(o) { Audio.play(if (o.win) "win" else "lose") }
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = true) { }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).clip(RoundedCornerShape(32.dp)).background(Color.White).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(o.title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            for (l in o.lines) Text(l, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64), textAlign = TextAlign.Center)
            for (b in o.badges) Text("🏅 Lencana baru: ${b.emoji} ${b.title}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CPurple, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            BigButton(primary, CGreen, Modifier.fillMaxWidth()) { onPrimary() }
            Spacer(Modifier.height(10.dp))
            BigButton(secondary, COrange, Modifier.fillMaxWidth()) { onSecondary() }
        }
    }
}

@Composable
fun Intro(title: String, emoji: String, text: String, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(emoji, fontSize = 80.sp)
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01579B), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color(0xFF37474F), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        BigButton("Mula!", CGreen, Modifier.fillMaxWidth(0.7f), "▶", fontSize = 28.sp) { onStart() }
    }
}

@Composable
fun HoldButton(label: String, color: Color, size: Dp, onChange: (Boolean) -> Unit) {
    var down by remember { mutableStateOf(false) }
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(color.copy(alpha = if (down) 0.95f else 0.65f))
            .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    down = true; onChange(true)
                    try { tryAwaitRelease() } finally { down = false; onChange(false) }
                })
            },
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}
