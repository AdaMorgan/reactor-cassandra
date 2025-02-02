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

configure<SourceSetContainer> {
    register("examples") {
        java.srcDir("src/examples/java")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

dependencies {
    //Apache Cassandra Native Driver
    implementation(libs.cassandra)

    //Logger
    api(libs.slf4j)

    //Collections Utility
    api(libs.commons.collections)

    compileOnly(libs.jna)

    //General Utility
    implementation(libs.trove4j)
    implementation(libs.commons.text)
    implementation(libs.jetbrains.annotations)

    testImplementation(libs.junit)
    testImplementation(libs.commons.lang3)

    //Sets the dependencies for the examples
    configurations["examplesImplementation"].withDependencies {
        addAll(configurations["api"].allDependencies)
        addAll(configurations["implementation"].allDependencies)
        addAll(configurations["compileOnly"].allDependencies)
    }

    implementation(files("libs/DataLib.jar"))
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

    if (javaVersion.isJava11) {
        args.add("--release")
        args.add("11")
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