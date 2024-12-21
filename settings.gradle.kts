rootProject.name = "reactor-cassandra"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("cassandra",             "com.datastax.cassandra",   "cassandra-driver-core"    ).version("3.11.5")
            library("logback-classic",       "ch.qos.logback",           "logback-classic"          ).version("1.5.6")
            library("jna",                   "net.java.dev.jna",         "jna"                      ).version("5.14.0")
            library("trove4j",               "net.sf.trove4j",           "core"                     ).version("3.1.0")
            library("commons-collections",   "org.apache.commons",       "commons-collections4"     ).version("4.4")
            library("commons-lang3",         "org.apache.commons",       "commons-lang3"            ).version("3.14.0")
            library("jetbrains-annotations", "org.jetbrains",            "annotations"              ).version("24.1.0")
            library("junit",                 "org.junit.jupiter",        "junit-jupiter"            ).version("5.10.2")
            library("slf4j",                 "org.slf4j",                "slf4j-api"                ).version("2.0.13")
        }
    }
}

