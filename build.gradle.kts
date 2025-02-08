plugins {
    kotlin("multiplatform") version "2.1.0"
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    mavenCentral()
}

kotlin {
    group = "de.westnordost"
    version = "0.2.0"

    jvm()
    js {
        browser()
        nodejs()
    }
    linuxX64()
    linuxArm64()
    mingwX64()

    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

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

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
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