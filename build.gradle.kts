import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("org.jetbrains.dokka") version "2.1.0"
}

repositories {
    mavenCentral()
}

group = "de.westnordost"
version = "0.3.0"

kotlin {

    jvm()

    js(IR) {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
      browser()
      nodejs()
      d8()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
      nodejs()
    }

    // native tier 1
    macosArm64()
    iosSimulatorArm64()
    iosArm64()

    // native tier 2
    macosX64()
    iosX64()
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()

    // native tier 3
    mingwX64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    watchosDeviceArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
            }
        }
        jvmTest {
            dependencies {
                implementation("ch.poole:OpeningHoursParser:0.28.2")
                implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
            }
        }
    }
}

dokka {
    moduleName.set("OSM Opening Hours")
    dokkaSourceSets {
        configureEach {
            sourceLink {
                remoteUrl("https://github.com/westnordost/osm-opening-hours/tree/v${project.version}/")
                localDirectory = rootDir
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), rootProject.name, version.toString())

    pom {
        name = "osm-opening-hours"
        description = "OpenStreetMap opening hours schema parser and generator"
        inceptionYear = "2024"
        url = "https://github.com/westnordost/osm-opening-hours"
        licenses {
            license {
                name = "MIT License"
                url = "https://raw.githubusercontent.com/westnordost/osm-opening-hours/master/LICENSE.txt"
            }
        }
        issueManagement {
            system = "GitHub"
            url = "https://github.com/westnordost/osm-opening-hours/issues"
        }
        scm {
            connection = "https://github.com/westnordost/osm-opening-hours.git"
            url = "https://github.com/westnordost/osm-opening-hours"
            developerConnection = connection
        }
        developers {
            developer {
                id = "westnordost"
                name = "Tobias Zwick"
                email = "osm@westnordost.de"
            }
        }
    }
}

tasks.register<UpdateOpeningHoursTask>("updateOpeningHours") {
    targetFiles = listOf(
        "src/jvmTest/resources/opening_hours_counts_jvm.tsv",
        "src/commonTest/resources/opening_hours_counts.tsv",
    )
}
