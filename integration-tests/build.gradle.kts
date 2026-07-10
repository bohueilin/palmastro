plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":contracts"))
    implementation(project(":engine-scan-quality"))
    implementation(project(":engine-palm-features"))
    implementation(project(":engine-astro"))
    implementation(project(":engine-scoring"))
    implementation(project(":engine-content"))
    implementation(project(":svc-analytics"))
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
    // Forward the parity-fixture output dir into the forked test JVM.
    System.getProperty("palmastro.fixtures.dir")?.let { systemProperty("palmastro.fixtures.dir", it) }
}
