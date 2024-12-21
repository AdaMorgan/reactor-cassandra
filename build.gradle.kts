plugins {
    signing
    `java-library`
    `maven-publish`

    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "reactor-cassandra"
val javaVersion = JavaVersion.current()

repositories {
    mavenCentral()
}

dependencies {
    //Apache Cassandra Native Driver

    //Code safety
    compileOnly(libs.jetbrains.annotations)

    //Logger
    api(libs.slf4j)

    //Collections Utility
    api(libs.commons.collections)

    compileOnly(libs.jna)

    //General Utility
    implementation(libs.cassandra)
    implementation(libs.trove4j)

    testImplementation(libs.junit)
    testImplementation(libs.commons.lang3)
    testImplementation(libs.logback.classic)
}

tasks.test {
    useJUnitPlatform()
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