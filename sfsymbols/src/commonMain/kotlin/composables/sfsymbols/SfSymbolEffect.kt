package com.composables.sfsymbols

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** How often an effect plays. `null` repeat on [SfSymbolIcon] means the effect's natural behaviour. */
public sealed interface SfEffectRepeat {
    public data object Once : SfEffectRepeat
    public data class Times(val count: Int) : SfEffectRepeat
    public data object Continuous : SfEffectRepeat
}

/** Whether an effect acts on the symbol as one piece or layer by layer (staggered). */
public enum class SfEffectScope { WholeSymbol, ByLayer }

public enum class SfBounceDirection { Up, Down }
public enum class SfRotateDirection { Clockwise, CounterClockwise }
public enum class SfVariableColorStyle { Iterative, Cumulative }

public sealed interface SfWiggleDirection {
    public data object Left : SfWiggleDirection
    public data object Right : SfWiggleDirection
    public data object Up : SfWiggleDirection
    public data object Down : SfWiggleDirection
    public data object Forward : SfWiggleDirection
    public data object Backward : SfWiggleDirection
    public data object Clockwise : SfWiggleDirection
    public data object CounterClockwise : SfWiggleDirection

    /** Movement along [degrees], counter-clockwise from +x. */
    public data class Custom(val degrees: Float) : SfWiggleDirection
}

/**
 * Symbol effects, mirroring Apple's `SymbolEffect` set. Pass one to [SfSymbolIcon].
 *
 * Timing, easing curves, keyframe values and per-layer stagger come from the SF Symbols app's own
 * animation recipes.
 *
 * - *Discrete* effects play when [SfSymbolIcon]'s `trigger` changes (and once on first show).
 * - *Indefinite* ones (Breathe, continuous Rotate, and any effect with `repeat = Continuous`) run while `active` is true.
 * - *State* effects (Appear, Disappear, Scale, DrawOn, DrawOff) hold their end state.
 */
public sealed class SfSymbolEffect {
    public class Appear(public val scope: SfEffectScope = SfEffectScope.WholeSymbol) : SfSymbolEffect()
    public class Disappear(public val scope: SfEffectScope = SfEffectScope.WholeSymbol) : SfSymbolEffect()
    public class Bounce(
        public val direction: SfBounceDirection = SfBounceDirection.Up,
        public val scope: SfEffectScope = SfEffectScope.WholeSymbol,
    ) : SfSymbolEffect()

    /** Dims the symbol's pulse layers (all layers when none are flagged) to 30 % and back over 2 s. */
    public class Pulse(public val scope: SfEffectScope = SfEffectScope.WholeSymbol) : SfSymbolEffect()
    public class VariableColor(
        public val style: SfVariableColorStyle = SfVariableColorStyle.Iterative,
        public val reversing: Boolean = false,
        public val hideInactiveLayers: Boolean = false,
    ) : SfSymbolEffect()

    public class Scale(public val up: Boolean = true) : SfSymbolEffect()

    /** `direction = null` uses the symbol's own preferred wiggle direction. */
    public class Wiggle(public val direction: SfWiggleDirection? = null) : SfSymbolEffect()

    /**
     * `direction = null` uses the symbol's preferred rotation direction. `continuous = true` spins at a
     * steady one turn per two seconds until stopped.
     */
    public class Rotate(
        public val direction: SfRotateDirection? = null,
        public val scope: SfEffectScope = SfEffectScope.WholeSymbol,
        public val continuous: Boolean = false,
    ) : SfSymbolEffect()

    public class Breathe(public val pulsing: Boolean = false) : SfSymbolEffect()
    public class DrawOn(public val scope: SfEffectScope = SfEffectScope.WholeSymbol) : SfSymbolEffect()
    public class DrawOff(public val scope: SfEffectScope = SfEffectScope.WholeSymbol) : SfSymbolEffect()
}

/** Per-layer result of evaluating an effect at one instant. Offsets are in em (the symbol's point size), as in Apple's recipes. */
internal class SfFx {
    var scale = 1f
    var dx = 0f
    var dy = 0f
    var rot = 0f
    var alpha = 1f
    var draw = 1f
    var pivotX = Float.NaN
    var pivotY = Float.NaN
    var visible = true

    fun reset() {
        scale = 1f; dx = 0f; dy = 0f; rot = 0f; alpha = 1f; draw = 1f
        pivotX = Float.NaN; pivotY = Float.NaN; visible = true
    }
}

private fun clamp01(x: Float) = if (x < 0f) 0f else if (x > 1f) 1f else x

/**
 * Apple's timing model, as found in the SF Symbols app: every animated property is a list of segments,
 * each "animate to a value over a duration along a cubic-Bezier(x1, y1, x2, y2)".
 */
internal class SfCurve(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    /** Eased progress for linear progress [p] (0..1). */
    fun ease(p: Float): Float {
        val x = clamp01(p)
        if (x == 0f || x == 1f) return x
        var lo = 0f
        var hi = 1f
        var t = x
        repeat(24) { // solve bezier-x(t) = x by bisection
            val u = 1f - t
            val bx = 3f * u * u * t * x1 + 3f * u * t * t * x2 + t * t * t
            if (bx < x) lo = t else hi = t
            t = (lo + hi) / 2f
        }
        val u = 1f - t
        return 3f * u * u * t * y1 + 3f * u * t * t * y2 + t * t * t
    }
}

private val EASE = SfCurve(0.33f, 0f, 0.67f, 1f)
private val BOUNCE_A = SfCurve(0.55f, 0f, 0.67f, 1f)
private val BOUNCE_B = SfCurve(0.45f, 0f, 0.67f, 1f)
private val BOUNCE_C = SfCurve(0.25f, 0f, 0.55f, 1f)
private val WIGGLE_1 = SfCurve(0.55f, 0f, 0.67f, 1f)
private val BREATHE_SCALE = SfCurve(0.4f, 0f, 0.6f, 1f)
private val ROTATE_ONCE = SfCurve(0.25f, 0.25f, 0.25f, 1f)
private val ROTATE_LINEAR = SfCurve(0.17f, 0.17f, 0.83f, 0.83f)
private val DRAW_ON = SfCurve(0.2f, 0f, 0.25f, 1f)
private val DRAW_OFF = SfCurve(0.4f, 0f, 0.8f, 1f)

private const val BOUNCE_UP_SEG = 0.21667f
private const val BOUNCE_TOTAL = 0.21667f + 0.21667f + 0.26667f
private const val WIGGLE_SEG = 0.167f
private val WIGGLE_MOVE = floatArrayOf(0.06f, -0.04f, 0.03f, -0.025f, 0f)   // em
private val WIGGLE_TURN = floatArrayOf(10f, -8f, 6f, -5f, 0f)                // degrees
private const val VC_SLOT = 0.3333f

/**
 * Apple's draw duration for a stroke of [length] points (100 pt symbol): 8..60 frames at 60 fps,
 * `(52 * g(u) + 8) / 60` with `g(u) = ((-0.77u + 0.96)u + 0.81)u` and `u` solving
 * `0.7u^3 - 0.27u^2 + 0.57u = length / 581.79`.
 */
internal fun drawSeconds(length: Float): Float {
    val x = length / 581.79f
    var lo = 0f
    var hi = 4f
    repeat(30) {
        val u = (lo + hi) / 2f
        if ((0.7f * u - 0.27f) * u * u + 0.57f * u < x) lo = u else hi = u
    }
    val u = (lo + hi) / 2f
    return (52f * (((-0.77f * u + 0.96f) * u + 0.81f) * u) + 8f) / 60f
}

/** Replace transition (Apple's down-up recipe): the old symbol disappears, then the new one appears. */
internal const val REPLACE_SECONDS = 0.1667f + 0.25f
internal fun replaceOut(t: Float, fx: SfFx) {
    val p = EASE.ease(t / 0.1667f); fx.alpha = 1f - p; fx.scale = 1f - 0.25f * p
}
internal fun replaceIn(t: Float, fx: SfFx) {
    val p = EASE.ease((t - 0.1667f) / 0.25f); fx.alpha = p; fx.scale = 0.75f + 0.25f * p
}

private fun segment(c: SfCurve, from: Float, to: Float, t0: Float, dur: Float, t: Float): Float =
    from + (to - from) * c.ease((t - t0) / dur)

internal fun SfSymbolEffect.defaultRepeat(): SfEffectRepeat = when {
    this is SfSymbolEffect.Breathe -> SfEffectRepeat.Continuous
    this is SfSymbolEffect.Rotate && continuous -> SfEffectRepeat.Continuous
    else -> SfEffectRepeat.Once
}

/** Effects that leave the symbol in a changed state after playing. */
internal fun SfSymbolEffect.isHeld(): Boolean = this is SfSymbolEffect.Appear || this is SfSymbolEffect.Disappear ||
    this is SfSymbolEffect.Scale || this is SfSymbolEffect.DrawOn || this is SfSymbolEffect.DrawOff

/** Seconds one animation group needs to draw (on) / erase (off). */
private fun groupDrawSeconds(symbol: SfSymbol, group: Int, off: Boolean): Float =
    symbol.layers.filter { it.group == group }.maxOfOrNull {
        if (it.rootU.isNaN()) (if (off) 0.1667f else 0.25f) else drawSeconds(it.strokeLength)
    } ?: 0f

private fun drawStart(symbol: SfSymbol, group: Int, off: Boolean): Float =
    (0 until group).sumOf { groupDrawSeconds(symbol, it, off).toDouble() }.toFloat()

private fun layerStep(maxStep: Float, budget: Float, groups: Int): Float = min(maxStep, budget / groups)

/** Per-layer alpha / draw fractions for a variable value (0..1), e.g. the lit bars of wifi. */
internal class SfVariableLevels(val alpha: FloatArray, val draw: FloatArray)

/** Variable layers in the order they light up. */
internal fun sfVariableOrder(symbol: SfSymbol): List<Int> =
    variableLayers(symbol.layers).let { if (symbol.backToFront) it else it.reversed() }

/**
 * Layer `pos` of `m` lights once the value passes `pos / m` (0 shows none, 1 shows all); inactive layers dim to 30 %.
 * Layers that support variable draw are instead drawn by the fraction `value * m - pos`.
 */
internal fun sfVariableLevels(symbol: SfSymbol, value: Float): SfVariableLevels {
    val layers = symbol.layers
    val seq = sfVariableOrder(symbol)
    val m = max(1, seq.size)
    val v = value.coerceIn(0f, 1f)
    val alpha = FloatArray(layers.size) { 1f }
    val draw = FloatArray(layers.size) { 1f }
    seq.forEachIndexed { pos, i ->
        if (layers[i].variableDraw) draw[i] = (v * m - pos).coerceIn(0f, 1f)
        else alpha[i] = if (v > pos.toFloat() / m) 1f else 0.3f
    }
    return SfVariableLevels(alpha, draw)
}

private fun variableLayers(layers: List<SfLayer>): List<Int> {
    val auto = layers.indices.filter { layers[it].autoVariable }
    return auto.ifEmpty { layers.indices.filter { layers[it].hierarchicalLevel >= 0 && !layers[it].erases } }
}

/** Total seconds one play of [e] takes for [symbol]. */
internal fun effectDuration(e: SfSymbolEffect, symbol: SfSymbol): Float = when (e) {
    is SfSymbolEffect.Appear -> 0.25f
    is SfSymbolEffect.Disappear -> 0.1667f
    is SfSymbolEffect.Bounce -> BOUNCE_TOTAL
    is SfSymbolEffect.Pulse -> 2.0f
    is SfSymbolEffect.VariableColor -> {
        val k = max(1, variableLayers(symbol.layers).size)
        (k + 1) * VC_SLOT * (if (e.reversing) 2f else 1f)
    }
    is SfSymbolEffect.Scale -> 0.25f
    is SfSymbolEffect.Wiggle -> 5 * WIGGLE_SEG
    is SfSymbolEffect.Rotate -> 2.0f
    is SfSymbolEffect.Breathe -> 3.0f
    is SfSymbolEffect.DrawOn, is SfSymbolEffect.DrawOff -> {
        val stroked = symbol.layers.filter { !it.rootU.isNaN() }
        val off = e is SfSymbolEffect.DrawOff
        val byLayer = (e as? SfSymbolEffect.DrawOn)?.scope == SfEffectScope.ByLayer ||
            (e as? SfSymbolEffect.DrawOff)?.scope == SfEffectScope.ByLayer
        if (byLayer) drawStart(symbol, symbol.groupCount, off)
        else max(0.25f, stroked.maxOfOrNull { drawSeconds(it.strokeLength) } ?: 0f)
    }
}

/** Evaluates [e] at time [t] (seconds) for layer [i] of [symbol] into [fx]. */
internal fun applyEffect(e: SfSymbolEffect, t: Float, i: Int, symbol: SfSymbol, fx: SfFx) {
    val layers = symbol.layers
    val groups = symbol.groupCount
    val gi = layers[i].group
    when (e) {
        is SfSymbolEffect.Appear -> {
            val d = if (e.scope == SfEffectScope.ByLayer) gi * layerStep(0.05f, 0.15f, groups) else 0f
            val p = EASE.ease((t - d) / (0.25f - d))
            fx.alpha = p
            fx.scale = 0.75f + 0.25f * p
        }
        is SfSymbolEffect.Disappear -> {
            val d = if (e.scope == SfEffectScope.ByLayer) gi * layerStep(0.0333f, 0.1f, groups) else 0f
            val p = EASE.ease((t - d) / (0.1667f - d))
            fx.alpha = 1f - p
            fx.scale = 1f - 0.25f * p
        }
        is SfSymbolEffect.Bounce -> {
            val d = if (e.scope == SfEffectScope.ByLayer) gi * layerStep(0.05f, 0.15f, groups) else 0f
            val tl = t - d
            val up = e.direction == SfBounceDirection.Up
            // offset multipliers 1.25 -> 0.9094 -> 1.0 (down mirrors: 0.75 -> 1.1088 -> 1.0)
            val v1 = if (up) 1.25f else 0.75f
            val v2 = if (up) 0.9094f else 1.1088f
            val v = when {
                tl <= 0f -> 1f
                tl < BOUNCE_UP_SEG -> segment(BOUNCE_A, 1f, v1, 0f, BOUNCE_UP_SEG, tl)
                tl < 2 * BOUNCE_UP_SEG -> segment(BOUNCE_B, v1, v2, BOUNCE_UP_SEG, BOUNCE_UP_SEG, tl)
                else -> segment(BOUNCE_C, v2, 1f, 2 * BOUNCE_UP_SEG, 0.26667f - d, tl)
            }
            fx.dy = -(v - 1f)
        }
        is SfSymbolEffect.Pulse -> {
            if (!layers.any { it.canPulse } || layers[i].canPulse) {
                val d = if (e.scope == SfEffectScope.ByLayer) gi * 0.05f else 0f
                val tl = t - d
                fx.alpha = when {
                    tl <= 0f -> 1f
                    tl < 1f -> segment(EASE, 1f, 0.3f, 0f, 1f, tl)
                    else -> segment(EASE, 0.3f, 1f, 1f, 1f, tl)
                }
            }
        }
        is SfSymbolEffect.VariableColor -> {
            val seq = sfVariableOrder(symbol)
            val pos = seq.indexOf(i)
            if (pos >= 0) {
                val total = (seq.size + 1) * VC_SLOT
                var tt = t
                if (e.reversing && tt > total) tt = 2 * total - tt
                val inactive = if (e.hideInactiveLayers) 0f else 0.3f
                val up = EASE.ease((tt - pos * VC_SLOT) / VC_SLOT)
                val level = when (e.style) {
                    SfVariableColorStyle.Cumulative -> up
                    SfVariableColorStyle.Iterative -> up * (1f - EASE.ease((tt - (pos + 1) * VC_SLOT) / VC_SLOT))
                }
                fx.alpha = inactive + (1f - inactive) * level
            }
        }
        is SfSymbolEffect.Scale -> {
            val p = EASE.ease(t / 0.25f)
            fx.scale = 1f + (if (e.up) 0.25f else -0.25f) * p
        }
        is SfSymbolEffect.Wiggle -> {
            // damped oscillation: five 0.167 s segments with Apple's amplitudes
            fun wave(a: FloatArray): Float {
                if (t >= 5 * WIGGLE_SEG) return 0f
                val k = min(4, (t / WIGGLE_SEG).toInt().coerceAtLeast(0))
                val prev = if (k == 0) 0f else a[k - 1]
                val c = if (k == 0) WIGGLE_1 else EASE
                return prev + (a[k] - prev) * c.ease((t - k * WIGGLE_SEG) / WIGGLE_SEG)
            }
            when (val dir = e.direction ?: preferredWiggle(symbol)) {
                SfWiggleDirection.Left, SfWiggleDirection.Backward -> fx.dx = -wave(WIGGLE_MOVE)
                SfWiggleDirection.Right, SfWiggleDirection.Forward -> fx.dx = wave(WIGGLE_MOVE)
                SfWiggleDirection.Up -> fx.dy = -wave(WIGGLE_MOVE)
                SfWiggleDirection.Down -> fx.dy = wave(WIGGLE_MOVE)
                SfWiggleDirection.Clockwise -> fx.rot = wave(WIGGLE_TURN)
                SfWiggleDirection.CounterClockwise -> fx.rot = -wave(WIGGLE_TURN)
                is SfWiggleDirection.Custom -> {
                    val a = dir.degrees * PI.toFloat() / 180f
                    val w = wave(WIGGLE_MOVE)
                    fx.dx = cos(a) * w; fx.dy = -sin(a) * w
                }
            }
        }
        is SfSymbolEffect.Rotate -> {
            if (!layers.any { it.canRotate } || layers[i].canRotate) {
                val cw = (e.direction ?: if (symbol.glyph.rotation == 2) SfRotateDirection.CounterClockwise
                else SfRotateDirection.Clockwise) == SfRotateDirection.Clockwise
                val d = if (e.scope == SfEffectScope.ByLayer) gi * layerStep(0.05f, 0.2f, groups + 1) else 0f
                val ang = if (e.continuous) 360f * (((t / 2f) % 1f))
                else segment(ROTATE_ONCE, 0f, 360f, d, 2f - d, t)
                fx.rot = if (cw) ang else -ang
                val a = symbol.glyph.anchors
                if (a.size >= 2) {
                    var ax = 0f; var ay = 0f; val c = a.size / 2
                    for (k in 0 until c) { ax += a[k * 2]; ay += a[k * 2 + 1] }
                    fx.pivotX = ax / c; fx.pivotY = 1f - ay / c
                }
            }
        }
        is SfSymbolEffect.Breathe -> {
            val tt = t % 3.0f
            fx.scale = if (tt < 1.5f) segment(BREATHE_SCALE, 1f, 1.2f, 0f, 1.5f, tt)
            else segment(BREATHE_SCALE, 1.2f, 1f, 1.5f, 1.5f, tt)
            if (e.pulsing) fx.alpha = if (tt < 1.5f) segment(EASE, 1f, 0.3f, 0f, 1.5f, tt)
            else segment(EASE, 0.3f, 1f, 1.5f, 1.5f, tt)
        }
        is SfSymbolEffect.DrawOn -> {
            val d = if (e.scope == SfEffectScope.ByLayer) drawStart(symbol, gi, false) else 0f
            if (layers[i].rootU.isNaN()) {            // no stroke: scales and fades in like Appear
                val p = EASE.ease((t - d) / 0.25f); fx.alpha = p; fx.scale = 0.75f + 0.25f * p
            } else fx.draw = DRAW_ON.ease((t - d) / drawSeconds(layers[i].strokeLength))
        }
        is SfSymbolEffect.DrawOff -> {
            val d = if (e.scope == SfEffectScope.ByLayer) drawStart(symbol, gi, true) else 0f
            if (layers[i].rootU.isNaN()) {            // no stroke: scales and fades out like Disappear
                val p = EASE.ease((t - d) / 0.1667f); fx.alpha = 1f - p; fx.scale = 1f - 0.25f * p
            } else fx.draw = 1f - DRAW_OFF.ease((t - d) / drawSeconds(layers[i].strokeLength))
        }
    }
}

private fun preferredWiggle(symbol: SfSymbol): SfWiggleDirection = when (symbol.glyph.wiggle) {
    1 -> SfWiggleDirection.Right
    2 -> SfWiggleDirection.Left
    3 -> SfWiggleDirection.Up
    4 -> SfWiggleDirection.Down
    5 -> SfWiggleDirection.Clockwise
    6 -> SfWiggleDirection.CounterClockwise
    7 -> SfWiggleDirection.Backward
    8 -> SfWiggleDirection.Forward
    9 -> SfWiggleDirection.Custom(symbol.glyph.wiggleAngle.toFloat())
    else -> SfWiggleDirection.Right
}
