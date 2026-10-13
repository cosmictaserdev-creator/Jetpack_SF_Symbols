package com.composables.sfsymbols

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import com.composables.sfsymbols.core.SfData
import com.composables.sfsymbols.core.SfGlyph
import com.composables.sfsymbols.core.SfInfo
import com.composables.sfsymbols.core.SfNode

/** The nine SF Symbols weights. [wght] is the value on the font's variable weight axis. */
public enum class SfWeight(internal val wght: Float) {
    Ultralight(28.93f), Thin(112.72f), Light(276.31f), Regular(400f), Medium(508f),
    Semibold(590.8f), Bold(700f), Heavy(858.4f), Black(1000f),
}

public enum class SfRenderingMode { Monochrome, Hierarchical, Palette, Multicolor }

/** One animatable layer of a symbol (a leaf of Apple's layer tree). */
public class SfLayer internal constructor(
    internal val contours: IntArray,
    /** -1 = not drawn in hierarchical mode, 0 primary, 1 secondary, 2 tertiary. */
    public val hierarchicalLevel: Int,
    internal val erases: Boolean,
    internal val colorName: String?,
    internal val opacity: Float,
    public val canPulse: Boolean,
    public val canRotate: Boolean,
    internal val autoVariable: Boolean,
    internal val rootU: Float,
    internal val rootV: Float,
    /** Approximate centre-line length of the layer's strokes, in points at a 100 pt symbol. */
    internal val strokeLength: Float,
    /** Index of the animation group this layer belongs to; layers of one group animate together. */
    internal val group: Int,
)

public class SfCategory internal constructor(public val key: String, public val label: String, public val icon: String)

/**
 * An SF Symbol with real Apple name, weights, layers and animation hints. Geometry is decoded lazily.
 *
 * ```kotlin
 * val s = SfSymbol.named("heart.fill")!!
 * SfSymbolIcon(s, size = 28.dp, effect = SfSymbolEffect.Bounce())
 * ```
 */
public class SfSymbol internal constructor(private val info: SfInfo) {
    public val name: String get() = info.name

    /** Property name from the original per-icon API, e.g. `SFHeartFill`. */
    public val legacyName: String get() = SfData.legacyName(info.name)
    public val keywords: List<String> get() = info.keywords
    public val categories: List<SfCategory> get() = info.categories.map { categoryList[it] }

    /** First OS-release year it shipped, as Apple lists it ("2019", "2022.1", ...). */
    public val since: String get() = info.since
    public val isRestricted: Boolean get() = info.restricted
    public val mirrorsInRtl: Boolean get() = info.mirrorsInRtl
    public val supportsHierarchical: Boolean get() = info.supportsHierarchical
    public val supportsMulticolor: Boolean get() = info.supportsMulticolor

    internal val glyph: SfGlyph by lazy { SfData.glyph(info) }

    /** True when layers come straight from Apple's layer data (false: one merged layer). */
    public val hasExactLayers: Boolean get() = glyph.layersExact
    internal val backToFront: Boolean get() = glyph.backToFront

    /** Number of animation groups: layers sharing a group move together, others are staggered. */
    internal val groupCount: Int by lazy { (layers.maxOfOrNull { it.group } ?: 0) + 1 }

    /** Erase layers (knock-outs) need layered drawing even in plain monochrome. */
    internal val hasErasers: Boolean by lazy { layers.any { it.erases } }

    public val layers: List<SfLayer> by lazy { flatten(glyph.tree) }

    private fun flatten(tree: List<SfNode>): List<SfLayer> {
        val lengths = contourLengths()
        val out = ArrayList<SfLayer>()
        var group = 0
        fun walk(n: SfNode, g: Int) {
            if (n.children.isEmpty()) {
                out += SfLayer(
                    n.contours, n.hierarchical, n.monoErase, SfData.colorName(n.colorId), n.opacity,
                    n.canPulse, n.canRotate, n.autoVariable || n.variableDraw, n.rootU, n.rootV,
                    n.contours.sumOf { c -> lengths.getOrElse(c) { 0f }.toDouble() }.toFloat() / 2f / 2048f * 100f, g,
                )
            } else {
                val gg = if (n.animateTogether) group++ else -1
                for (c in n.children) walk(c, if (gg >= 0) gg else group++)
            }
        }
        for (n in tree) walk(n, group++)
        return out
    }

    /** Outline length of each contour at Regular weight, in font units. */
    private fun contourLengths(): FloatArray {
        val g = glyph
        val p = points(SfWeight.Regular.wght)
        val out = ArrayList<Float>()
        var k = 0
        var cx = 0f; var cy = 0f
        var acc = 0f
        for (op in g.ops) {
            when (op.toInt()) {
                0 -> { cx = p[k]; cy = p[k + 1]; k += 2 }
                1 -> { acc += kotlin.math.hypot(p[k] - cx, p[k + 1] - cy); cx = p[k]; cy = p[k + 1]; k += 2 }
                2 -> {
                    var px = cx; var py = cy
                    for (i in 1..8) {
                        val t = i / 8f; val u = 1 - t
                        val x = u * u * u * cx + 3 * u * u * t * p[k] + 3 * u * t * t * p[k + 2] + t * t * t * p[k + 4]
                        val y = u * u * u * cy + 3 * u * u * t * p[k + 1] + 3 * u * t * t * p[k + 3] + t * t * t * p[k + 5]
                        acc += kotlin.math.hypot(x - px, y - py); px = x; py = y
                    }
                    cx = p[k + 4]; cy = p[k + 5]; k += 6
                }
                else -> { out += acc; acc = 0f }
            }
        }
        return out.toFloatArray()
    }

    internal fun points(weight: SfWeight): FloatArray = points(weight.wght)

    /** Interpolated point list, y flipped to screen orientation (y down). */
    internal fun points(wght: Float): FloatArray {
        val g = glyph
        val n: Float
        val src: FloatArray
        if (wght >= 400f) { n = (wght - 400f) / 600f; src = g.hi } else { n = (400f - wght) / 399f; src = g.lo }
        val out = FloatArray(g.reg.size)
        for (i in out.indices step 2) {
            out[i] = g.reg[i] + n * (src[i] - g.reg[i])
            out[i + 1] = -(g.reg[i + 1] + n * (src[i + 1] - g.reg[i + 1]))
        }
        return out
    }

    /** One [Path] per contour (screen orientation, font units). */
    internal fun contourPaths(weight: SfWeight): List<Path> {
        val g = glyph
        val pts = points(weight)
        val paths = ArrayList<Path>()
        var cur = Path()
        var k = 0
        for (op in g.ops) {
            when (op.toInt()) {
                0 -> { cur.moveTo(pts[k], pts[k + 1]); k += 2 }
                1 -> { cur.lineTo(pts[k], pts[k + 1]); k += 2 }
                2 -> { cur.cubicTo(pts[k], pts[k + 1], pts[k + 2], pts[k + 3], pts[k + 4], pts[k + 5]); k += 6 }
                else -> { cur.close(); paths += cur; cur = Path() }
            }
        }
        return paths
    }

    /**
     * Tight bounds of the *visible* outline at [weight] (true curve extrema, not control points), y down.
     * Contours that only exist to knock out other layers are ignored, so ink stays centred.
     */
    public fun bounds(weight: SfWeight = SfWeight.Regular): Rect = boundsCache.getOrPut(this to weight) {
        val g = glyph
        val p = points(weight)
        val drawn = HashSet<Int>()
        for (l in layers) if (!l.erases) for (c in l.contours) drawn += c
        val all = drawn.isEmpty()
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        var contour = 0
        var skip = !all && contour !in drawn
        fun add(x: Float, y: Float) {
            if (skip) return
            if (x < l) l = x; if (x > r) r = x
            if (y < t) t = y; if (y > b) b = y
        }
        // extrema of a cubic on one axis: roots of the derivative in (0,1)
        fun cubicAxis(p0: Float, p1: Float, p2: Float, p3: Float, out: (Float) -> Unit) {
            val a = -p0 + 3 * p1 - 3 * p2 + p3
            val bb = 2 * (p0 - 2 * p1 + p2)
            val c = p1 - p0
            fun at(t: Float): Float { val u = 1 - t; return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3 }
            if (kotlin.math.abs(a) < 1e-6f) {
                if (kotlin.math.abs(bb) > 1e-9f) { val tt = -c / bb; if (tt > 0f && tt < 1f) out(at(tt)) }
            } else {
                val disc = bb * bb - 4 * a * c
                if (disc >= 0f) {
                    val sq = kotlin.math.sqrt(disc)
                    for (tt in floatArrayOf((-bb + sq) / (2 * a), (-bb - sq) / (2 * a))) if (tt > 0f && tt < 1f) out(at(tt))
                }
            }
        }
        var k = 0
        var cx = 0f; var cy = 0f
        for (op in g.ops) {
            when (op.toInt()) {
                0, 1 -> { cx = p[k]; cy = p[k + 1]; add(cx, cy); k += 2 }
                2 -> {
                    val x1 = p[k]; val y1 = p[k + 1]; val x2 = p[k + 2]; val y2 = p[k + 3]; val x3 = p[k + 4]; val y3 = p[k + 5]
                    add(x3, y3)
                    cubicAxis(cx, x1, x2, x3) { add(it, cy) }
                    cubicAxis(cy, y1, y2, y3) { add(cx, it) }
                    cx = x3; cy = y3; k += 6
                }
                else -> { contour++; skip = !all && contour !in drawn }
            }
        }
        Rect(l, t, r, b)
    }

    /**
     * Static monochrome [ImageVector] for APIs that need one (e.g. Material `Icon`). Use
     * [SfSymbolIcon] for layers, rendering modes and animation.
     */
    public fun toImageVector(weight: SfWeight = SfWeight.Regular): ImageVector {
        val g = glyph
        val pts = points(weight)
        val b = bounds(weight)
        val nodes = ArrayList<PathNode>()
        var k = 0
        for (op in g.ops) {
            when (op.toInt()) {
                0 -> { nodes += PathNode.MoveTo(pts[k] - b.left, pts[k + 1] - b.top); k += 2 }
                1 -> { nodes += PathNode.LineTo(pts[k] - b.left, pts[k + 1] - b.top); k += 2 }
                2 -> {
                    nodes += PathNode.CurveTo(
                        pts[k] - b.left, pts[k + 1] - b.top, pts[k + 2] - b.left, pts[k + 3] - b.top,
                        pts[k + 4] - b.left, pts[k + 5] - b.top,
                    ); k += 6
                }
                else -> nodes += PathNode.Close
            }
        }
        val scale = 24f / maxOf(b.width, b.height)
        return ImageVector.Builder(
            name = name, defaultWidth = (b.width * scale).dp, defaultHeight = (b.height * scale).dp,
            viewportWidth = b.width, viewportHeight = b.height,
        ).addPath(
            pathData = nodes, fill = SolidColor(Color.Black),
            // knock-out layers can't be 'cleared' in a vector; even-odd carves them out of the shapes beneath
            pathFillType = if (hasErasers) PathFillType.EvenOdd else PathFillType.NonZero,
        ).build()
    }

    override fun toString(): String = "SfSymbol($name)"
    override fun equals(other: Any?): Boolean = other is SfSymbol && other.info === info
    override fun hashCode(): Int = info.hashCode()

    public companion object {
        private val boundsCache = HashMap<Pair<SfSymbol, SfWeight>, Rect>()
        private val symbols: Map<SfInfo, SfSymbol> by lazy { SfData.all().associateWith { SfSymbol(it) } }
        private val categoryList: List<SfCategory> by lazy {
            val k = SfData.categoryKeys(); val l = SfData.categoryLabels(); val i = SfData.categoryIcons()
            k.indices.map { SfCategory(k[it], l[it], i[it]) }
        }

        /** Every symbol (8,500+), in Apple's order. */
        public val all: List<SfSymbol> by lazy { SfData.all().map { symbols.getValue(it) } }
        public val allCategories: List<SfCategory> get() = categoryList

        /**
         * Looks up by Apple name (`"heart.fill"`), Apple alias, or the original per-icon API name
         * (`"SFHeartFill"`). Returns null when unknown.
         */
        public fun named(name: String): SfSymbol? = SfData.find(name)?.let { symbols.getValue(it) }

        /** Throwing variant of [named]. */
        public operator fun get(name: String): SfSymbol = named(name) ?: error("Unknown SF Symbol: $name")

        /** Case-insensitive search over names, aliases-by-keyword and Apple's search keywords. */
        public fun search(query: String, category: SfCategory? = null): List<SfSymbol> {
            val q = query.trim().lowercase()
            val ci = category?.let { c -> categoryList.indexOf(c) }
            return all.filter { s ->
                (ci == null || ci in s.info.categories) &&
                    (q.isEmpty() || s.name.contains(q) || s.info.keywords.any { it.lowercase().contains(q) })
            }
        }
    }
}
