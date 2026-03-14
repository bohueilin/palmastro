pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "palmastro"

include(
    ":contracts",
    ":engine-scan-quality",
    ":engine-palm-features",
    ":engine-astro",
    ":engine-scoring",
    ":engine-content",
    ":svc-analytics",
    ":data-room",
    ":app",
    ":integration-tests",
)
