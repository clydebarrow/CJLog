/*
 * Copyright (c) 2019-2020 Control-J Pty. Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    }
}


plugins {
    signing
    idea
    `maven-publish`
    kotlin("jvm") version "1.7.20"
    id("org.jetbrains.dokka") version "1.5.0"
}

group = "com.control-j.cjlog"
version = "2.5-SNAPSHOT"

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp:4.10.0")
}

dependencies {
    implementation("io.reactivex.rxjava3:rxjava:3.1.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.12.2")
}

val javaVersion = JavaVersion.VERSION_1_8
tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}


val dokkaJar by tasks.creating(Jar::class) {
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
    dependsOn(tasks.dokkaHtml)
}

publishing {
    repositories {
        maven {
            val version = rootProject.version as String
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            setUrl(if (version.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = project.property("ossrhUsername") as String
                password = project.property("ossrhPassword") as String
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJar)
            groupId = rootProject.group as String
            version = rootProject.version as String
            artifactId = "core"

            pom {
                name.set("CJLog")
                description.set("A lightweight, system independent logging facility for multiple destinations")
                url.set("https://github.com/clydebarrow/CJLog")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("clyde")
                        name.set("Clyde Stubbs")
                        url.set("https://github.com/clydebarrow")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/clydebarrow/CJLog.git")
                    developerConnection.set("scm:git:https://github.com/clydebarrow/CJLog.git")
                    url.set("https://github.com/clydebarrow/CJLog")
                }
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
signing {
    sign(publishing.publications["maven"])
}

