package com.composables.sfsymbols

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** What happens visually when the `symbol` passed to [SfSymbolIcon] changes. */
public enum class SfSymbolTransition {
    /** Swap instantly. */
    None,

    /** Old symbol scales/fades out while the new one scales/fades in. */
    Replace,

    /** Morphs outline-to-outline when both symbols share a structure, otherwise behaves like [Replace]. */
    MagicReplace,
}

/**
 * Draws an SF Symbol with weight, rendering mode and (optionally) a symbol effect.
 *
 * @param size larger side of the symbol, in dp (the composable itself is a square of this size).
 * @param palette colours for [SfRenderingMode.Palette], primary first. Missing levels reuse the last one.
 * @param effect animation to play, or null.
 * @param repeat how often it plays; null = the effect's natural behaviour (Breathe loops, others play once).
 * @param trigger discrete effects replay whenever this value changes.
 * @param active pauses/stops continuous effects when false; for state effects (Appear, Disappear,
 * Scale, DrawOn, DrawOff) it applies (true) or reverts (false) the effect.
 * @param transition how a changed [symbol] is animated.
 */
@Composable
public fun SfSymbolIcon(
    symbol: SfSymbol,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    weight: SfWeight = SfWeight.Regular,
    mode: SfRenderingMode = SfRenderingMode.Monochrome,
    tint: Color = Color.Black,
    palette: List<Color> = emptyList(),
    effect: SfSymbolEffect? = null,
    repeat: SfEffectRepeat? = null,
    trigger: Any? = null,
    active: Boolean = true,
    transition: SfSymbolTransition = SfSymbolTransition.None,
    contentDescription: String? = null,
) {
    var current by remember { mutableStateOf(symbol) }
    var outgoing by remember { mutableStateOf<SfSymbol?>(null) }
    val swap = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(symbol, transition) {
        if (symbol == current) return@LaunchedEffect
        if (transition == SfSymbolTransition.None) {
            outgoing = null; current = symbol; swap.floatValue = 1f
        } else {
            outgoing = current; current = symbol; swap.floatValue = 0f
            swap.play(0f, 1f, REPLACE_SECONDS)
            outgoing = null
        }
    }

    val time = remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(effect, trigger, active, repeat, current) { runEffect(time, effect, current, repeat, active) }

    val geo = remember(current, weight) { SfGeometry(current, weight) }
    val outGeo = outgoing?.let { o -> remember(o, weight) { SfGeometry(o, weight) } }

    val sem = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription; role = Role.Image }
    } else Modifier
    Layout(
        modifier = modifier.then(sem).drawBehind {
            val t = time.floatValue
            val sw = swap.floatValue
            val o = outgoing
            if (o != null && outGeo != null && sw < 1f) {
                if (transition == SfSymbolTransition.MagicReplace && morphable(o, current)) {
                    drawMorph(o, current, weight, sw, tint)
                } else {
                    val fo = SfFx().also { replaceOut(sw * REPLACE_SECONDS, it) }
                    val fi = SfFx().also { replaceIn(sw * REPLACE_SECONDS, it) }
                    drawSymbol(outGeo, mode, tint, palette, null, -1f, alpha = fo.alpha, extraScale = fo.scale)
                    drawSymbol(geo, mode, tint, palette, effect, t, alpha = fi.alpha, extraScale = fi.scale)
                }
            } else {
                drawSymbol(geo, mode, tint, palette, effect, t, alpha = 1f, extraScale = 1f)
            }
        },
    ) { _, _ ->
        val px = size.roundToPx()
        layout(px, px) {}
    }
}

// ---------------------------------------------------------------------------------------------

private class SfGeometry(val symbol: SfSymbol, weight: SfWeight) {
    val contours: List<Path> = symbol.contourPaths(weight)
    val whole: Path = Path().also { p -> contours.forEach { p.addPath(it) } }
    val layerPaths: List<Path> = symbol.layers.map { l ->
        Path().also { p -> l.contours.forEach { c -> if (c < contours.size) p.addPath(contours[c]) } }
    }
    val layerBounds: List<Rect> = layerPaths.map { it.getBounds() }
    val bounds: Rect = symbol.bounds(weight)
}

/** Linear playback of a float from [from] to [to] over |to-from| seconds of wall time. */
private suspend fun MutableFloatState.play(from: Float, to: Float, seconds: Float) {
    floatValue = from
    if (from == to) return
    val start = withFrameNanos { it }
    while (true) {
        val el = (withFrameNanos { it } - start) / 1_000_000_000f
        if (el >= seconds) { floatValue = to; return }
        floatValue = from + (to - from) * (el / seconds)
    }
}

private suspend fun runEffect(
    time: MutableFloatState, effect: SfSymbolEffect?, symbol: SfSymbol, repeat: SfEffectRepeat?, active: Boolean,
) {
    if (effect == null) { time.floatValue = -1f; return }
    val dur = effectDuration(effect, symbol)
    if (effect.isHeld()) {
        val from = max(0f, time.floatValue)
        val to = if (active) dur else 0f
        time.play(from, to, abs(to - from).coerceAtLeast(0.001f))
        return
    }
    if (!active) { time.floatValue = -1f; return }
    when (val r = repeat ?: effect.defaultRepeat()) {
        SfEffectRepeat.Continuous -> while (true) time.play(0f, dur, dur)
        is SfEffectRepeat.Times -> repeat(max(1, r.count)) { time.play(0f, dur, dur) }
        SfEffectRepeat.Once -> time.play(0f, dur, dur)
    }
    time.floatValue = -1f
}

private fun layerColor(layer: SfLayer, mode: SfRenderingMode, tint: Color, palette: List<Color>): Color? {
    if (layer.colorName == "hidden") return null
    return when (mode) {
        SfRenderingMode.Monochrome -> if (layer.erases) null else tint
        SfRenderingMode.Hierarchical -> {
            val lvl = layer.hierarchicalLevel
            if (lvl < 0) null else tint.copy(alpha = tint.alpha * HIER_ALPHA[min(lvl, 2)])
        }
        SfRenderingMode.Palette -> {
            val lvl = layer.hierarchicalLevel
            if (lvl < 0) null
            else palette.getOrNull(lvl) ?: palette.lastOrNull() ?: tint.copy(alpha = tint.alpha * HIER_ALPHA[min(lvl, 2)])
        }
        SfRenderingMode.Multicolor -> {
            val c = sfNamedColor(layer.colorName, tint)
            c?.copy(alpha = c.alpha * layer.opacity)
        }
    }
}

/** Font units per em: Apple's offsets are in em. */
private const val EM = 2048f

private val HIER_ALPHA = floatArrayOf(1f, 0.5f, 0.25f)

private fun hex(v: Long) = Color((0xFF000000L or v).toInt())

internal fun sfNamedColor(name: String?, tint: Color): Color? = when (name) {
    null, "tintColor", "labelColor", "controlAccentColor", "_gradient.highlegibility" -> tint
    "erase", "hidden" -> null
    "white" -> Color.White
    "black" -> Color.Black
    "secondaryLabelColor" -> tint.copy(alpha = tint.alpha * 0.5f)
    "tertiaryLabelColor" -> tint.copy(alpha = tint.alpha * 0.3f)
    "systemRedColor" -> hex(0xFF3B30)
    "systemOrangeColor" -> hex(0xFF9500)
    "systemYellowColor" -> hex(0xFFCC00)
    "systemGreenColor" -> hex(0x34C759)
    "systemMintColor" -> hex(0x00C7BE)
    "systemTealColor" -> hex(0x30B0C7)
    "systemCyanColor" -> hex(0x32ADE6)
    "systemBlueColor" -> hex(0x007AFF)
    "systemIndigoColor" -> hex(0x5856D6)
    "systemPurpleColor" -> hex(0xAF52DE)
    "systemPinkColor" -> hex(0xFF2D55)
    "systemBrownColor" -> hex(0xA2845E)
    "systemGrayColor" -> hex(0x8E8E93)
    else -> if (name.startsWith("#") && name.length == 7) hex(name.substring(1).toLong(16)) else tint
}

private fun DrawScope.drawSymbol(
    geo: SfGeometry, mode: SfRenderingMode, tint: Color, palette: List<Color>,
    effect: SfSymbolEffect?, time: Float, alpha: Float, extraScale: Float,
) {
    val b = geo.bounds
    val maxDim = max(b.width, b.height)
    if (maxDim <= 0f) return
    val s = min(size.width, size.height) / maxDim * extraScale
    val ox = (size.width - b.width * s) / 2f - b.left * s
    val oy = (size.height - b.height * s) / 2f - b.top * s
    val playing = effect != null && (time >= 0f || effect.isHeld())
    val layered = mode != SfRenderingMode.Monochrome || playing || alpha < 1f || geo.symbol.hasErasers
    withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
        if (!layered) {
            drawPath(geo.whole, tint)
            return@withTransform
        }
        val sym = geo.symbol
        val layers = sym.layers
        val pad = maxDim
        drawContext.canvas.saveLayer(Rect(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad), Paint().apply { this.alpha = alpha })
        val fx = SfFx()
        val tt = max(0f, time)
        for (i in layers.indices) {
            fx.reset()
            if (playing) applyEffect(effect, tt, i, sym, fx)
            if (fx.alpha <= 0.001f) continue
            val lb = geo.layerBounds[i]
            val px = if (fx.pivotX.isNaN()) b.center.x else b.left + fx.pivotX * b.width
            val py = if (fx.pivotY.isNaN()) b.center.y else b.top + fx.pivotY * b.height
            val pivot = Offset(px, py)
            withTransform({
                translate(fx.dx * EM, fx.dy * EM)
                if (fx.rot != 0f) rotate(fx.rot, pivot)
                if (fx.scale != 1f) scale(fx.scale, fx.scale, pivot)
            }) {
                val layer = layers[i]
                val path = geo.layerPaths[i]
                if (layer.erases) drawPath(path, Color.Black, blendMode = BlendMode.Clear)
                val c = layerColor(layer, mode, tint, palette)
                if (c != null) {
                    val col = c.copy(alpha = c.alpha * fx.alpha)
                    if (fx.draw < 0.999f) {
                        if (!layer.rootU.isNaN()) {
                            // stroke grows outward from its start point (first rail tie)
                            val cx = b.left + layer.rootU * b.width
                            val cy = b.top + layer.rootV * b.height
                            val far = maxOf(
                                hypot(cx - lb.left, cy - lb.top), hypot(cx - lb.right, cy - lb.top),
                                hypot(cx - lb.left, cy - lb.bottom), hypot(cx - lb.right, cy - lb.bottom),
                            )
                            val r = far * fx.draw
                            val disc = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
                            clipPath(disc) { drawPath(path, col) }
                        } else {
                            drawPath(path, col.copy(alpha = col.alpha * fx.draw))
                        }
                    } else drawPath(path, col)
                }
            }
        }
        drawContext.canvas.restore()
    }
}

private fun morphable(a: SfSymbol, b: SfSymbol): Boolean =
    a.glyph.ops.contentEquals(b.glyph.ops)

private fun DrawScope.drawMorph(a: SfSymbol, b: SfSymbol, weight: SfWeight, f: Float, tint: Color) {
    val pa = a.points(weight)
    val pb = b.points(weight)
    val pts = FloatArray(pa.size) { pa[it] + (pb[it] - pa[it]) * f }
    val path = Path()
    var k = 0
    for (op in a.glyph.ops) when (op.toInt()) {
        0 -> { path.moveTo(pts[k], pts[k + 1]); k += 2 }
        1 -> { path.lineTo(pts[k], pts[k + 1]); k += 2 }
        2 -> { path.cubicTo(pts[k], pts[k + 1], pts[k + 2], pts[k + 3], pts[k + 4], pts[k + 5]); k += 6 }
        else -> path.close()
    }
    val bb = path.getBounds()
    val maxDim = max(bb.width, bb.height)
    if (maxDim <= 0f) return
    val s = min(size.width, size.height) / maxDim
    withTransform({
        translate((size.width - bb.width * s) / 2f - bb.left * s, (size.height - bb.height * s) / 2f - bb.top * s)
        scale(s, s, Offset.Zero)
    }) { drawPath(path, tint) }
}
