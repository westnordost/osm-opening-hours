plugins {
    kotlin("multiplatform") version "2.1.0"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.14"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":"))
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
            implementation("io.ktor:ktor-client-core:3.3.1")
            implementation("io.ktor:ktor-client-okhttp:3.3.1")
            implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.14")
            implementation("dev.sargunv.kotlin-dsv:kotlin-dsv:0.4.0")
            implementation("ch.poole:OpeningHoursParser:0.28.2")
        }
    }
}

benchmark {
    configurations {
        named("main") {
            iterations = 5
        }
    }

    targets {
        register("jvm")
    }
}
