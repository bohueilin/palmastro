plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
}

subprojects {
    group = "com.palmastro"
    version = "0.1.0"

    apply(plugin = "io.gitlab.arturbosch.detekt")

    repositories {
        mavenCentral()
        google()
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        // Pre-launch debt is frozen per-module; detekt gates only NEW findings (maxIssues=0).
        baseline = file("detekt-baseline.xml")
    }
}
