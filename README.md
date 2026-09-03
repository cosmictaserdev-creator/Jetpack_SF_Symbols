# SF Symbols for Jetpack Compose

A high-performance Jetpack Compose and Compose Multiplatform icon library bringing all 7,007 Apple SF Symbols to Android and Kotlin Multiplatform applications as pure, tree-shakeable `ImageVector` definitions.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.02.00-green.svg?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![SF Symbols](https://img.shields.io/badge/SF%20Symbols-7.3%20(7007)-black.svg?style=flat-square&logo=apple&logoColor=white)](https://developer.apple.com/sf-symbols/)
[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-Support-FF5E5B.svg?style=flat-square&logo=kofi&logoColor=white)](https://ko-fi.com/cosmictaser)
[![Website](https://img.shields.io/badge/Website-cosmictaser.de5.net-8A2BE2.svg?style=flat-square&logo=googlechrome&logoColor=white)](https://cosmictaser.de5.net)

---

## Overview

SF Symbols for Jetpack Compose is a vector icon framework engineered specifically for modern Android and Compose Multiplatform development. It provides access to the complete SF Symbols collection (7,007 symbols in Dualtone and Monochrome variants, totaling 14,014 generated vectors) with native `ImageVector` performance, zero memory overhead at startup, and complete R8/ProGuard dead-code elimination.

Designed to mirror the official `androidx.compose.material.icons.Icons` API conventions, this library delivers seamless autocompletion, effortless theming, and dynamic tinting.

### Keywords
`jetpack-compose-sf-symbols`, `sf-symbols-android`, `compose-multiplatform-icons`, `apple-sf-symbols-compose`, `sf-symbols-kotlin`, `compose-vector-icons`, `android-sf-symbols`, `ios-icons-for-android`, `sf-symbols-library`

---

## Key Highlights

- **Complete Collection (7,007 Symbols)**: Full coverage of the SF Symbols 7 dataset with high-precision vector paths.
- **Dualtone and Monochrome Modes**: Dualtone mode provides layered visual depth using alpha channels (`0.21` secondary and `0.85` primary fills), while Monochrome mode delivers clean flat geometry.
- **Zero Startup Allocation**: Every icon uses private backing properties and is only instantiated upon first render.
- **Automatic Dead-Code Stripping**: Release builds compiled with R8 / ProGuard will strip out every icon that is not explicitly referenced in your code.
- **Compose Multiplatform Ready**: Fully compatible with Android (`minSdk 21`), Compose Desktop (JVM), iOS, and Web (Wasm/JS).
- **Searchable Metadata Catalog**: Built-in runtime lookup index (`SfSymbolsCatalog`) with Apple symbol name mapping, categories, and restriction statuses.
- **Interactive Visual Browser**: Includes a standalone, offline-ready HTML vector catalog browser (`preview.html`) for previewing all 7,007 icons directly in VS Code or web browsers.

---

## Installation

### Gradle (Kotlin DSL)

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependency to your application's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.cosmictaserdev-creator.Jetpack_SF_Symbols:sfsymbols:1.0.0")
}
```

*For detailed setup instructions (including local module import), refer to [INSTALLATION.md](INSTALLATION.md).*

---

## Quick Start

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFHeartFill
import com.composables.sfsymbols.dualtone.SFCheckmarkCircleFill
import com.composables.sfsymbols.monochrome.SFStarFill

@Composable
fun QuickStartExample() {
    // Dualtone Icon (Depth with layered opacities)
    Icon(
        imageVector = SfSymbols.Dualtone.SFHeartFill,
        contentDescription = "Favorite",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
    )

    // Dualtone Icon with custom color tint
    Icon(
        imageVector = SfSymbols.Dualtone.SFCheckmarkCircleFill,
        contentDescription = "Completed",
        tint = Color(0xFF34C759),
        modifier = Modifier.size(24.dp)
    )

    // Monochrome Icon (Flat fill)
    Icon(
        imageVector = SfSymbols.Monochrome.SFStarFill,
        contentDescription = "Star",
        tint = Color(0xFFFF9500),
        modifier = Modifier.size(24.dp)
    )
}
```

---

## Render Variants

### Dualtone (Default)
Dualtone renders multi-layer vectors with subtle opacity shifts. When passing a `tint` color, both layers blend proportionally against the tint:

```kotlin
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFFolderFill

Icon(
    imageVector = SfSymbols.Dualtone.SFFolderFill,
    contentDescription = "Folder",
    tint = Color(0xFF007AFF)
)
```

### Monochrome
Monochrome renders a solid, single-layer vector shape:

```kotlin
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFFolderFill

Icon(
    imageVector = SfSymbols.Monochrome.SFFolderFill,
    contentDescription = "Folder",
    tint = Color(0xFF007AFF)
)
```

---

## In-App Search & Runtime Catalog

SF Symbols Compose includes `SfSymbolsCatalog` for in-app icon pickers, searches, and Apple symbol identifier resolution:

```kotlin
import com.composables.sfsymbols.SfSymbolsCatalog

// Search for symbols matching a keyword
val results = SfSymbolsCatalog.search("wifi")

// Resolve metadata from an Apple SF Symbol name
val symbol = SfSymbolsCatalog.findByAppleName("square.and.arrow.up")
// symbol?.pascalName == "SFSquareAndArrowUp"
// symbol?.categories == ["General", "Share"]
```

---

## Documentation

- **[Installation Guide (INSTALLATION.md)](INSTALLATION.md)**: Gradle setup, composite builds, local module imports, and ProGuard configuration.
- **[Usage Guide (USAGE.md)](USAGE.md)**: Material 3 integration (Buttons, Navigation Bars, FABs), custom Canvas drawing, and Multiplatform guidelines.
- **[Interactive Catalog (preview.html)](preview.html)**: Standalone 7,007-symbol visual search browser for VS Code and web browsers.

---

## Supporting the Project

If you find this library useful in your projects, consider supporting its continued maintenance and development:

- **Ko-fi**: [ko-fi.com/cosmictaser](https://ko-fi.com/cosmictaser)
- **UPI Support**: Available via Ko-fi or direct UPI
- **Website**: [cosmictaser.de5.net](https://cosmictaser.de5.net)
- **Discord Community**: [Join the Discord](https://discord.gg/Ejeb4cmzfd)

---

## Attribution & Credits

- **Upstream Origin**: Derived and transformed from [`sf-symbols-lib`](https://github.com/phranck/sf-symbols-lib) by [phranck](https://github.com/phranck) and the [sf-symbols-lib-jetpackCompose](https://github.com/cosmictaserdev-creator/sf-symbols-lib-jetpackCompose) repository.
- **Symbol Design**: SF Symbols glyph design, naming hierarchy, and vector curves are copyrighted by Apple Inc.

---

## Legal Disclaimer

This library is developed strictly for **educational, interoperability, and design research purposes**.

SF Symbols is a trademark of Apple Inc. Please review Apple's [SF Symbols License Agreement](https://developer.apple.com/sf-symbols/) regarding proper usage guidelines in non-Apple software environments.

The code generator and Kotlin wrapper bindings are open-sourced under the [MIT License](LICENSE).
