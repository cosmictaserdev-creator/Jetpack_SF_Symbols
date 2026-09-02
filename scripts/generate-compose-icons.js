/**
 * SF Symbols to Jetpack Compose Generator
 *
 * Converts all 7,007 Dualtone and 7,007 Monochrome SF Symbols from TSX SVG definitions
 * into 100% native, tree-shakeable Jetpack Compose ImageVector Kotlin properties.
 *
 * Usage:
 *   node scripts/generate-compose-icons.js
 */
const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const repoSourceDir = path.join(rootDir, 'repo_source');
const outputBaseDir = path.join(rootDir, 'sfsymbols', 'src', 'main', 'java', 'com', 'composables', 'sfsymbols');

const MODES = [
  { name: 'dualtone', packageName: 'com.composables.sfsymbols.dualtone', subObject: 'Dualtone' },
  { name: 'monochrome', packageName: 'com.composables.sfsymbols.monochrome', subObject: 'Monochrome' }
];

function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
}

function parseIconTsx(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const svgMatch = content.match(/const SVG_CONTENT = '(.*?)';/);
  const viewBoxMatch = content.match(/const VIEW_BOX = '(.*?)';/);
  
  if (!svgMatch || !viewBoxMatch) return null;
  
  const svg = svgMatch[1];
  const viewBoxParts = viewBoxMatch[1].split(' ').map(Number);
  const viewportWidth = viewBoxParts[2] || 24;
  const viewportHeight = viewBoxParts[3] || 24;
  
  const paths = [];
  const pathRegex = /<path\s+([^>]+)\/?>/g;
  let match;
  while ((match = pathRegex.exec(svg)) !== null) {
    const attrStr = match[1];
    const dMatch = attrStr.match(/d="([^"]+)"/);
    const opacityMatch = attrStr.match(/fill-opacity="([^"]+)"/);
    if (dMatch) {
      paths.push({
        d: dMatch[1],
        opacity: opacityMatch ? parseFloat(opacityMatch[1]) : 1.0
      });
    }
  }
  
  return {
    viewportWidth,
    viewportHeight,
    paths
  };
}

function generateKotlinFile(pascalName, modeInfo, iconData) {
  const { packageName, subObject, name: modeName } = modeInfo;
  const backingFieldName = `_${pascalName.charAt(0).toLowerCase() + pascalName.slice(1)}`;

  const pathsCode = iconData.paths.map(p => {
    const alphaStr = p.opacity < 0.999 ? `${p.opacity}f` : '1.0f';
    const escapedD = p.d.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    if (alphaStr === '1.0f') {
      return `            addSfPath("${escapedD}")`;
    } else {
      return `            addSfPath("${escapedD}", fillAlpha = ${alphaStr})`;
    }
  }).join('\n');

  return `package ${packageName}

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.addSfPath
import com.composables.sfsymbols.sfIcon

/**
 * SF Symbol: ${pascalName} (${modeName})
 * Viewport: ${iconData.viewportWidth} x ${iconData.viewportHeight}
 */
public val SfSymbols.${subObject}.${pascalName}: ImageVector
    get() {
        if (${backingFieldName} != null) {
            return ${backingFieldName}!!
        }
        ${backingFieldName} = sfIcon(
            name = "${subObject}.${pascalName}",
            viewportWidth = ${iconData.viewportWidth}f,
            viewportHeight = ${iconData.viewportHeight}f
        ) {
${pathsCode}
        }
        return ${backingFieldName}!!
    }

private var ${backingFieldName}: ImageVector? = null
`;
}

function run() {
  console.log('🚀 Starting SF Symbols to Jetpack Compose Conversion...');
  
  // Create base core files
  ensureDir(outputBaseDir);

  // Write SfSymbols root object
  const sfSymbolsCore = `package com.composables.sfsymbols

/**
 * Entry point for SF Symbols icon collection for Jetpack Compose.
 *
 * Usage:
 * \`\`\`kotlin
 * Icon(
 *     imageVector = SfSymbols.Dualtone.SFHeartFill,
 *     contentDescription = "Favorite",
 *     tint = MaterialTheme.colorScheme.primary
 * )
 * \`\`\`
 */
public object SfSymbols {
    /** Dualtone variant with layered opacities for depth (Default) */
    public object Dualtone

    /** Monochrome variant with single uniform fill */
    public object Monochrome
}
`;
  fs.writeFileSync(path.join(outputBaseDir, 'SfSymbols.kt'), sfSymbolsCore);

  // Write helper DSL
  const sfHelperCore = `package com.composables.sfsymbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Internal helper to construct an SF Symbol ImageVector efficiently.
 */
internal inline fun sfIcon(
    name: String,
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Float = 24f,
    defaultHeight: Float = 24f,
    block: ImageVector.Builder.() -> Unit
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = defaultWidth.dp,
        defaultHeight = defaultHeight.dp,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight
    ).apply(block).build()
}

/**
 * Internal helper to add an SVG path with opacity to an ImageVector builder.
 */
internal inline fun ImageVector.Builder.addSfPath(
    pathData: String,
    fillAlpha: Float = 1.0f,
    fillColor: Color = Color.Black
): ImageVector.Builder {
    return addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(fillColor),
        fillAlpha = fillAlpha
    )
}
`;
  fs.writeFileSync(path.join(outputBaseDir, 'SfIconBuilder.kt'), sfHelperCore);

  // Process modes
  for (const mode of MODES) {
    const srcDir = path.join(repoSourceDir, 'src', mode.name, 'icons');
    const outDir = path.join(outputBaseDir, mode.name);
    ensureDir(outDir);

    if (!fs.existsSync(srcDir)) {
      console.warn(`⚠️ Source directory not found: ${srcDir}`);
      continue;
    }

    const files = fs.readdirSync(srcDir).filter(f => f.endsWith('.tsx'));
    console.log(`\n⏳ Converting ${files.length} ${mode.name} icons...`);

    let count = 0;
    for (const file of files) {
      const pascalName = file.replace('.tsx', '');
      const iconData = parseIconTsx(path.join(srcDir, file));
      if (!iconData) {
        console.warn(`⚠️ Could not parse: ${file}`);
        continue;
      }

      const ktContent = generateKotlinFile(pascalName, mode, iconData);
      fs.writeFileSync(path.join(outDir, `${pascalName}.kt`), ktContent);
      count++;
    }
    console.log(`✅ Finished ${count} ${mode.name} icons in ${outDir}`);
  }

  // Generate Catalog
  const catalogTsPath = path.join(repoSourceDir, 'docs', 'src', 'lib', 'catalog.ts');
  if (fs.existsSync(catalogTsPath)) {
    const content = fs.readFileSync(catalogTsPath, 'utf8');
    const entryRegex = /\{\s*name:\s*"([^"]+)",\s*pascalName:\s*"([^"]+)",\s*categories:\s*(\[[^\]]*\]),\s*restricted:\s*(true|false)\s*\}/g;
    let match;
    const entries = [];
    while ((match = entryRegex.exec(content)) !== null) {
      entries.push({
        appleName: match[1],
        pascalName: match[2],
        categories: JSON.parse(match[3]),
        restricted: match[4] === 'true'
      });
    }

    const catalogKt = `package com.composables.sfsymbols

/**
 * Metadata for an SF Symbol.
 */
public data class SfSymbolMetadata(
    val appleName: String,
    val pascalName: String,
    val categories: List<String>,
    val isRestricted: Boolean
)

/**
 * Complete catalog of all 7,007 SF Symbols with search and category indexing.
 */
public object SfSymbolsCatalog {
    public val all: List<SfSymbolMetadata> by lazy {
        listOf(
${entries.map(e => `            SfSymbolMetadata(appleName = "${e.appleName}", pascalName = "${e.pascalName}", categories = listOf(${e.categories.map(c => `"${c}"`).join(', ')}), isRestricted = ${e.restricted})`).join(',\n')}
        )
    }

    private val nameIndex: Map<String, SfSymbolMetadata> by lazy {
        all.associateBy { it.pascalName }
    }

    private val appleNameIndex: Map<String, SfSymbolMetadata> by lazy {
        all.associateBy { it.appleName }
    }

    public fun findByPascalName(name: String): SfSymbolMetadata? = nameIndex[name]

    public fun findByAppleName(name: String): SfSymbolMetadata? = appleNameIndex[name]

    public fun search(query: String): List<SfSymbolMetadata> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.appleName.lowercase().contains(q) ||
            it.pascalName.lowercase().contains(q) ||
            it.categories.any { cat -> cat.lowercase().contains(q) }
        }
    }
}
`;

    fs.writeFileSync(path.join(outputBaseDir, 'SfSymbolsCatalog.kt'), catalogKt);
    console.log(`✅ Generated SfSymbolsCatalog.kt with ${entries.length} symbols`);
  }

  console.log('\n🎉 Complete Jetpack Compose icon library generation finished successfully!');
}

run();
