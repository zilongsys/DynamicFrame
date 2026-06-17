import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val versionPropsFile = file("version.properties")

fun readVersionProps(): Properties {
    val props = Properties()
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { props.load(it) }
    } else {
        props["VERSION_MAJOR"] = "0"
        props["VERSION_MIDDLE"] = "1"
        props["VERSION_PATCH"] = "0"
        props["VERSION_CODE"] = "1"
    }
    return props
}

fun writeVersionProps(props: Properties) {
    versionPropsFile.outputStream().use {
        props.store(it, "DynamicFrame — PATCH 0-100, al llegar a 100 sube MIDDLE y PATCH=0")
    }
}

fun bumpVersionProps(props: Properties) {
    var major = props.getProperty("VERSION_MAJOR", "0").toInt()
    var middle = props.getProperty("VERSION_MIDDLE", "0").toInt()
    var patch = props.getProperty("VERSION_PATCH", "0").toInt()
    val code = props.getProperty("VERSION_CODE", "1").toInt() + 1

    if (patch < 100) {
        patch++
    } else {
        patch = 0
        if (middle < 100) {
            middle++
        } else {
            middle = 0
            major++
        }
    }

    props["VERSION_MAJOR"] = major.toString()
    props["VERSION_MIDDLE"] = middle.toString()
    props["VERSION_PATCH"] = patch.toString()
    props["VERSION_CODE"] = code.toString()
}

tasks.register("bumpVersion") {
    group = "dynamicframe"
    description = "Incrementa PATCH y VERSION_CODE en app/version.properties (al entregar cambios)."
    doLast {
        val props = readVersionProps()
        bumpVersionProps(props)
        writeVersionProps(props)
        val name = "${props["VERSION_MAJOR"]}.${props["VERSION_MIDDLE"]}.${props["VERSION_PATCH"]}"
        println("Versión actualizada: v$name (code ${props["VERSION_CODE"]})")
    }
}

val versionProps = readVersionProps()
val dfVersionName =
    "${versionProps.getProperty("VERSION_MAJOR")}.${versionProps.getProperty("VERSION_MIDDLE")}.${versionProps.getProperty("VERSION_PATCH")}"
val dfVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()

android {
    namespace = "com.dynamicframe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dynamicframe"
        minSdk = 23
        targetSdk = 35
        versionCode = dfVersionCode
        versionName = dfVersionName
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("boolean", "DEBUG_TOOLS_DEFAULT", "true")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "DEBUG_TOOLS_DEFAULT", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "target"
    productFlavors {
        create("tv") {
            dimension = "target"
            buildConfigField("Boolean", "IS_TV", "true")
        }
        create("mobile") {
            dimension = "target"
            buildConfigField("Boolean", "IS_TV", "false")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.leanback:leanback:1.2.0-alpha04")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-datasource:1.3.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")
    implementation("com.github.skydoves:landscapist-coil:2.3.6")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.documentfile:documentfile:1.0.1")
}
