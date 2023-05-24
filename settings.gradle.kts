
rootProject.name = "cdm-kotlin"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("coroutines-version", "1.6.4")

            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines-version")
            library("guava", "com.google.guava:guava:31.1-jre") // LOOK what used for ??

            //// logging
            library("microutils-logging", "io.github.microutils:kotlin-logging:3.0.4")
            library("logback-classic", "ch.qos.logback:logback-classic:1.3.4")

            // property based testing
            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines-version")
            library("kotest-property", "io.kotest", "kotest-property").version("5.5.4")        }
    }
}

include("clibs")
include("core")
include("testdata")