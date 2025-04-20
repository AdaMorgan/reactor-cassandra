rootProject.name = "reactor-cassandra"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("cassandra",             "com.datastax.cassandra",   "cassandra-driver-core"          ).version("3.11.5")

            library("trove4j",               "net.sf.trove4j",           "core"                           ).version("3.1.0")
            library("jsr",                   "com.google.code.findbugs", "jsr305"                         ).version("3.0.2")

            library("agrona",                "org.agrona",               "agrona"                         ).version("1.20.0")

            library("micrometer-core",       "io.micrometer",            "micrometer-core"                ).version("1.14.4")
            library("micrometer-tracing",    "io.micrometer",            "micrometer-tracing"             ).version("1.4.3")
            library("micrometer-bridge",     "io.micrometer",            "micrometer-tracing-bridge-brave").version("1.4.3")

            library("commons-collections",   "org.apache.commons",       "commons-collections4"           ).version("4.4")
            library("commons-lang3",         "org.apache.commons",       "commons-lang3"                  ).version("3.14.0")
            library("commons-text",          "org.apache.commons",       "commons-text"                   ).version("1.13.0")
            library("netty",                 "io.netty",                 "netty-all"                      ).version("4.1.75.Final")
            library("junit",                 "org.junit.jupiter",        "junit-jupiter"                  ).version("5.10.2")
            library("slf4j",                 "org.slf4j",                "slf4j-api"                      ).version("2.0.0")
        }
    }
}

