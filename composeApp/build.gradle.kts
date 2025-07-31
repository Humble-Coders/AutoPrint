import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation("com.google.firebase:firebase-admin:9.2.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            implementation("com.google.cloud:google-cloud-storage:2.29.1")
            //implementation("io.coil-kt:coil:3.0.0-alpha06")
            implementation("org.apache.pdfbox:pdfbox:3.0.1")
            implementation("io.ktor:ktor-client-core:2.3.5")
            implementation("io.ktor:ktor-client-cio:2.3.5")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")


            // extended icons
            implementation(compose.materialIconsExtended)
        }
    }
}


compose.desktop {
    application {
        mainClass = "org.example.autoprint.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.autoprint"
            packageVersion = "1.0.0"
        }
    }
}
