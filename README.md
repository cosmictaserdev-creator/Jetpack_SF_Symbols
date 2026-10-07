<p align="center">
  <img src="docs/images/hero.png" alt="SF Symbols rendered in Jetpack Compose" width="100%">
</p>

<h1 align="center">SF Symbols for Jetpack Compose</h1>

**Browse symbols on your PC:** [Download the ready-to-use SF Symbol Catalog](https://github.com/cosmictaserdev-creator/SF_Symbol_Catalog/releases/latest) for Windows, macOS, or Linux. Search symbols, save favorites, preview variants, then copy a symbol name or Compose snippet for this library. See the [catalog source and usage](https://github.com/cosmictaserdev-creator/SF_Symbol_Catalog).

<p align="center">
  All 7,007 Apple SF Symbols as native <code>ImageVector</code>s for Android and Compose Multiplatform.
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="MIT License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2025.02-4285F4.svg?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
  <a href="https://developer.apple.com/sf-symbols/"><img src="https://img.shields.io/badge/SF%20Symbols-7-000000.svg?style=flat-square&logo=apple&logoColor=white" alt="SF Symbols 7"></a>
  <a href="https://jitpack.io/#cosmictaserdev-creator/Jetpack_SF_Symbols"><img src="https://img.shields.io/badge/JitPack-1.0.4-2DCE89.svg?style=flat-square" alt="JitPack"></a>
</p>

<p align="center">
  <a href="#install">Install</a> &nbsp;&middot;&nbsp;
  <a href="#use">Use</a> &nbsp;&middot;&nbsp;
  <a href="#find-a-symbol">Find a symbol</a> &nbsp;&middot;&nbsp;
  <a href="USAGE.md">Full guide</a>
</p>

<br>

<table>
  <tr>
    <td width="33%"><img src="docs/images/whispry-home.png" alt="Whispry home screen"></td>
    <td width="33%"><img src="docs/images/whispry-presets.png" alt="Whispry presets screen"></td>
    <td width="33%"><img src="docs/images/whispry-settings.png" alt="Whispry settings screen"></td>
  </tr>
</table>

<p align="center"><sub>Whispry, a voice dictation app for Android, built with SF Symbols for Compose.</sub></p>

## Why this library

**Every symbol.** The full SF Symbols 7 set, 7,007 symbols, each in a Dualtone and a Monochrome version.

**Feels like Material Icons.** Same shape of API as `Icons.Filled.Home`, so autocomplete just works: `SfSymbols.Dualtone.SFHouseFill`.

**Costs nothing you do not use.** Icons are built lazily on first use, and R8 removes every symbol your code never references.

**Cross-platform.** Android (minSdk 21), JVM desktop, iOS devices and Apple silicon simulators, JavaScript, and WebAssembly.

## Install

**1. Add JitPack** to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

**2. Add the dependency** to your KMP module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("com.github.cosmictaserdev-creator.Jetpack_SF_Symbols:sfsymbols:KMP_RELEASE_TAG")
        }
    }
}
```

Replace `1.0.4` with the tag for a release that contains the KMP build; version `1.0.4` predates the multiplatform artifacts. For an Android-only app, use the same coordinate in its `dependencies { implementation(...) }` block.

**3. Sync Gradle.** That is it.

Prefer to vendor the source or use a composite build? See [INSTALLATION.md](INSTALLATION.md).

## Use

Pass a symbol to the regular Compose `Icon`:

```kotlin
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFHeartFill

Icon(
    imageVector = SfSymbols.Dualtone.SFHeartFill,
    contentDescription = "Favorite",
    tint = Color(0xFFFF375F),
    modifier = Modifier.size(28.dp)
)
```

Size it with `Modifier.size`, color it with `tint`. Nothing else to learn.

### Dualtone or Monochrome

Every symbol comes in two styles. Pick one by changing the package.

| Style | Import from | Looks like |
| :--- | :--- | :--- |
| Dualtone | `com.composables.sfsymbols.dualtone` | Two layers of the same tint at different opacity. Closest to Apple's hierarchical style. |
| Monochrome | `com.composables.sfsymbols.monochrome` | One solid shape. Best for small sizes and toolbars. |

```kotlin
import com.composables.sfsymbols.monochrome.SFFolderFill

Icon(SfSymbols.Monochrome.SFFolderFill, contentDescription = "Folder")
```

## Find a symbol

**Turn the Apple name into the Kotlin name.** Drop the dots, capitalize each word, add `SF` in front.

| Apple name | Kotlin name |
| :--- | :--- |
| `heart.fill` | `SFHeartFill` |
| `square.and.arrow.up` | `SFSquareAndArrowUp` |
| `figure.outdoor.cycle` | `SFFigureOutdoorCycle` |
| `battery.100percent` | `SFBattery100percent` |

**Browse visually.** Search the [SF Symbols app](https://developer.apple.com/sf-symbols/) on a Mac, then convert the name with the rule above.

**Search at runtime.** `SfSymbolsCatalog` lets you build your own icon picker:

```kotlin
import com.composables.sfsymbols.SfSymbolsCatalog

SfSymbolsCatalog.search("wifi")                          // every symbol matching "wifi"
SfSymbolsCatalog.findByAppleName("square.and.arrow.up")  // name, categories, restriction flag
```

## More docs

* [INSTALLATION.md](INSTALLATION.md) covers local modules, composite builds and R8.
* [USAGE.md](USAGE.md) covers Material 3 buttons, navigation bars, custom Canvas drawing and Multiplatform.

## Support

<p align="center">
  This library is free and built in spare time.<br>
  If it saved you an afternoon of exporting SVGs, a coffee keeps the updates coming.
</p>

<p align="center">
  <a href="https://ko-fi.com/cosmictaser"><img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white" alt="Support on Ko-fi" height="36"></a>
  &nbsp;
  <a href="https://discord.gg/Ejeb4cmzfd"><img src="https://img.shields.io/badge/Join%20the%20Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Join the Discord" height="36"></a>
  &nbsp;
  <a href="https://cosmictaser.de5.net"><img src="https://img.shields.io/badge/Website-111111?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Website" height="36"></a>
</p>

<p align="center">
  <sub>No money to spare? A star on this repo helps other developers find it.</sub>
</p>

## Credits and license

Built on [`sf-symbols-lib`](https://github.com/phranck/sf-symbols-lib) by [phranck](https://github.com/phranck) and [sf-symbols-lib-jetpackCompose](https://github.com/cosmictaserdev-creator/sf-symbols-lib-jetpackCompose).

The generator and Kotlin bindings are released under the [MIT License](LICENSE). The symbol artwork belongs to Apple Inc. and SF Symbols is an Apple trademark. Read Apple's [SF Symbols license](https://developer.apple.com/sf-symbols/) before shipping these icons in a product, since it limits their use outside Apple platforms. This project is meant for education, interoperability and design research.
