# Installation Guide — SF Symbols for Jetpack Compose

This guide provides technical instructions for integrating SF Symbols Compose into Android Studio, Gradle-based Android applications, and Kotlin Multiplatform projects.

---

## Integration Methods Overview

| Method | Recommended Use Case | Setup Complexity |
| :--- | :--- | :--- |
| **Method 1: JitPack / Remote Dependency** | Standard production applications and CI/CD pipelines | Minimal (Gradle declaration) |
| **Method 2: Local Project Module** | Offline environments or custom modifications | Moderate (Module inclusion) |
| **Method 3: Composite Gradle Build** | Multi-repository architectures and local development | Moderate (`includeBuild`) |

---

## Method 1: Remote Dependency (JitPack)

### Step 1: Configure Repository Resolution
In your project root `settings.gradle.kts`:

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

### Step 2: Add Dependency
In your module `build.gradle.kts` (e.g., `app/build.gradle.kts`):

```kotlin
dependencies {
    // SF Symbols Compose Core Library
    implementation("com.github.cosmictaserdev-creator:Jetpack_SF_Symbols:1.0.3")

    // Compose Dependencies
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
}
```

### Step 3: Sync Gradle
Select **Sync Project with Gradle Files** in Android Studio.

---

## Method 2: Local Module Integration

To embed the library source directly into your existing project:

### Step 1: Copy the Library Directory
Copy the `sfsymbols` folder into your Android project root:
```text
YourProject/
├── app/
├── sfsymbols/
├── build.gradle.kts
└── settings.gradle.kts
```

### Step 2: Include Module in Settings
In your `settings.gradle.kts`:

```kotlin
include(":app")
include(":sfsymbols")
```

### Step 3: Add Project Dependency
In your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sfsymbols"))
}
```

### Step 4: Sync
Sync your project to index all 7,007 vector definitions.

---

## Method 3: Gradle Composite Build

When maintaining this repository in a separate workspace directory:

In your application's `settings.gradle.kts`:

```kotlin
includeBuild("../Jetpack_SF_Symbols") {
    dependencySubstitution {
        substitute(module("com.github.cosmictaserdev-creator:Jetpack_SF_Symbols"))
            .using(project(":sfsymbols"))
    }
}
```

---

## ProGuard and R8 Optimization

SF Symbols Compose includes consumer ProGuard rules (`consumer-rules.pro`). 

Every icon vector is defined via lazy backing properties. During release compilation, R8 static analysis identifies and strips any unreferenced symbol from the compiled binary. No manual optimization rules are required.

---

## Verification Example

```kotlin
package com.example.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFCheckmarkCircleFill
import com.composables.sfsymbols.dualtone.SFHeartFill

@Composable
fun VerificationPreview() {
    Row {
        Icon(
            imageVector = SfSymbols.Dualtone.SFHeartFill,
            contentDescription = "Heart",
            tint = Color(0xFFFF2D55),
            modifier = Modifier.size(32.dp)
        )

        Icon(
            imageVector = SfSymbols.Dualtone.SFCheckmarkCircleFill,
            contentDescription = "Checkmark",
            tint = Color(0xFF34C759),
            modifier = Modifier.size(32.dp)
        )
    }
}
```
