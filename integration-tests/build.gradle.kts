plugins {
    kotlin("jvm")
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}
