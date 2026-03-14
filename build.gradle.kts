plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

subprojects {
    group = "com.palmastro"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}
