import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

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

tasks.withType<Test> {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    )
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
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
    failFast = true
}

val rebuild by tasks.creating(Task::class) {
    group = "build"

    dependsOn(build)
    dependsOn(tasks.clean)
    build.mustRunAfter(tasks.clean)
}