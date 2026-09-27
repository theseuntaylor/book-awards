import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.material.icons.core)
            implementation(compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// data/awards.json is the pipeline's output; merge it into the app's resources rather than keeping a copy in src.
val syncCommonResources by tasks.registering(Sync::class) {
    from("src/commonMain/composeResources")
    from(rootProject.file("data/awards.json")) { into("files") }
    into(layout.buildDirectory.dir("generated/bookAwardsResources/commonMain"))
}

compose.resources {
    packageOfResClass = "com.theseuntaylor.bookawards.resources"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = layout.dir(syncCommonResources.map { it.destinationDir })
    )
}

android {
    namespace = "com.theseuntaylor.bookawards"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.theseuntaylor.bookawards"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        // E2E checks point this at a local server: ./gradlew installDebug -PawardsDataUrl=http://localhost:8765/awards.json
        val awardsDataUrl = providers.gradleProperty("awardsDataUrl")
            .getOrElse("https://raw.githubusercontent.com/theseuntaylor/book-awards/main/data/awards.json")
        buildConfigField("String", "AWARDS_DATA_URL", "\"$awardsDataUrl\"")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
