import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.1.0"
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "2.1.0"
}

repositories {
    mavenCentral()
}

kotlin {
    group = "de.westnordost"
    version = "0.2.0"

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
            }
        }
        jvmTest {
            dependencies {
                implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
                implementation("ch.poole:OpeningHoursParser:0.28.2")
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

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationHtml.map { it.outputDirectory })
}

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = rootProject.name + if (name != "kotlinMultiplatform") "-$name" else ""
            artifact(javadocJar)
            pom {
                name.set("osm-opening-hours")
                description.set("OpenStreetMap opening hours schema parser and generator")
                url.set("https://github.com/westnordost/osm-opening-hours")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/westnordost/osm-opening-hours/master/LICENSE.txt")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/westnordost/osm-opening-hours/issues")
                }
                scm {
                    connection.set("https://github.com/westnordost/osm-opening-hours.git")
                    url.set("https://github.com/westnordost/osm-opening-hours")
                }
                developers {
                    developer {
                        id.set("westnordost")
                        name.set("Tobias Zwick")
                        email.set("osm@westnordost.de")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "oss"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                val ossrhUsername: String by project
                val ossrhPassword: String by project
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

// FIXME - workaround for https://github.com/gradle/gradle/issues/26091
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(signingTasks)
}
