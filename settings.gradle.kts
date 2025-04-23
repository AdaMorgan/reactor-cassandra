rootProject.name = "reactor-cassandra"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            plugin("publish",       "io.github.gradle-nexus.publish-plugin"     ).version("2.0.0")
            plugin("shadow",        "com.github.johnrengelman.shadow"           ).version("8.1.1")
            plugin("versions",      "com.github.ben-manes.versions"             ).version("0.52.0")

            library("trove4j",               "net.sf.trove4j",           "core"                           ).version("3.1.0")
            library("jsr",                   "com.google.code.findbugs", "jsr305"                         ).version("3.0.2")
            library("jetbrains-annotations", "org.jetbrains",            "annotations"                    ).version("26.0.2")

            library("micrometer-core",       "io.micrometer",            "micrometer-core"                ).version("1.14.4")
            library("micrometer-tracing",    "io.micrometer",            "micrometer-tracing"             ).version("1.4.3")
            library("micrometer-bridge",     "io.micrometer",            "micrometer-tracing-bridge-brave").version("1.4.3")

            library("commons-collections",   "org.apache.commons",       "commons-collections4"           ).version("4.4")
            library("commons-lang3",         "org.apache.commons",       "commons-lang3"                  ).version("3.14.0")
            library("commons-text",          "org.apache.commons",       "commons-text"                   ).version("1.13.0")
            library("netty",                 "io.netty",                 "netty-all"                      ).version("4.1.75.Final")
            library("slf4j",                 "org.slf4j",                "slf4j-api"                      ).version("2.0.17")

            //Test Implementation
            library("junit",                 "org.junit.jupiter",        "junit-jupiter"                  ).version("5.12.2")
            library("junit-launcher",        "org.junit.platform",       "junit-platform-launcher"        ).version("1.12.2")
            library("assertj",               "org.assertj",              "assertj-core"                   ).version("3.27.3")
            library("reflections",           "org.reflections",          "reflections"                    ).version("0.10.2")
            library("logback-classic",       "ch.qos.logback",           "logback-classic"                ).version("1.5.18")
            library("archunit",              "com.tngtech.archunit",     "archunit"                       ).version("1.4.0")
            library("mockito",               "org.mockito",              "mockito-core"                   ).version("5.17.0")

            bundle("junit", listOf("junit", "junit-launcher", "jetbrains-annotations"))
        }
    }
}

