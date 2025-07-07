/*
 * Copyright 2025 Ada Morgan, John Regan
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
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    signing
    `java-library`
    `maven-publish`

    alias(libs.plugins.publish)
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
}

group = "reactor-cassandra"

val javaVersion = JavaVersion.VERSION_1_8

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

configure<SourceSetContainer> {
    register("examples") {
        java.srcDir("src/examples/java")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

dependencies {

    //Code safety
    compileOnly(libs.jsr)
    compileOnly(libs.jetbrains.annotations)

    //Logger
    api(libs.slf4j)

    //Netty
    implementation(libs.netty)
    implementation(libs.bundles.netty.codec)

    //Metrics support
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.tracing)
    implementation(libs.micrometer.bridge)

    //Collections Utility
    api(libs.commons.collections)

    //General Utility
    implementation(libs.commons.text)
    implementation(libs.trove4j)

    //Sets the dependencies for the examples
    configurations["examplesImplementation"].withDependencies {
        addAll(configurations["api"].allDependencies)
        addAll(configurations["implementation"].allDependencies)
        addAll(configurations["compileOnly"].allDependencies)
    }

    testImplementation(libs.junit)
    testImplementation(libs.commons.lang3)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.reflections)
    testImplementation(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.commons.lang3)
    testImplementation(libs.logback.classic)
    testImplementation(libs.archunit)
}

tasks.test {
    useJUnitPlatform()
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }

    gradleReleaseChannel = "current"
}


val jar by tasks.getting(Jar::class) {
    archiveBaseName.set(project.name)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true

    val args = mutableListOf("-Xlint:deprecation", "-Xlint:unchecked")

    if (javaVersion.isJava9Compatible) {
        args.add("--release")
        args.add("8")
    }

    doFirst {
        options.compilerArgs = args
    }
}

val build by tasks.getting(Task::class) {
    dependsOn(jar)
    jar.mustRunAfter(tasks.clean)
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
    failFast = false
}

val updateTestSnapshots by tasks.registering(Test::class) {
    useJUnitPlatform()
    failFast = false

    systemProperty("updateSnapshots", "true")
}