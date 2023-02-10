
rootProject.name = "cdm-kotlin"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            library("guava", "com.google.guava:guava:31.1-jre")
            //// logging
            library("microutils-logging", "io.github.microutils:kotlin-logging:3.0.4")
            library("logback-classic", "ch.qos.logback:logback-classic:1.3.4")
        }
    }
}

