rootProject.name = "oap"

pluginManagement {
    plugins {
        id("oap.java-convention") version providers.gradleProperty("oap.java-convention.version").get()
    }
    repositories {
        maven { url = uri(providers.gradleProperty("altRepositoryUri")
            .getOrElse("https://maven.xenoss.net/repository/oap-maven/")) }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("altRepositoryUri")
            .getOrElse("https://maven.xenoss.net/repository/oap-maven/")) }
        mavenCentral()
    }
}

// top-level leaf modules
include("oap-stdlib")
include("oap-stdlib-test")
include("oap-jpath")
include("oap-highload")

// oap-http
include("oap-http:oap-http")
include("oap-http:oap-http-prometheus")
include("oap-http:oap-pnio-v3")
include("oap-http:oap-http-test")

// oap-mcp
include("oap-mcp:oap-mcp")
include("oap-mcp:oap-mcp-admin")
include("oap-mcp:oap-mcp-testing")

// oap-message
include("oap-message:oap-message-common")
include("oap-message:oap-message-client")
include("oap-message:oap-message-server")
include("oap-message:oap-message-test")

// oap-application
include("oap-application:oap-application-annotation")
include("oap-application:oap-application-cli")
include("oap-application:oap-application")
include("oap-application:oap-application-test")

// oap-formats
include("oap-formats:oap-tsv:oap-tsv")
include("oap-formats:oap-tsv:oap-tsv-test")
include("oap-formats:oap-json:oap-json-schema")
include("oap-formats:oap-template")
include("oap-formats:oap-template-test")
include("oap-formats:oap-logstream:oap-logstream")
include("oap-formats:oap-logstream:oap-logstream-data")
include("oap-formats:oap-logstream:oap-logstream-data-object")
include("oap-formats:oap-logstream:oap-logstream-net-server")
include("oap-formats:oap-logstream:oap-logstream-net-client")
include("oap-formats:oap-logstream:oap-logstream-test")

// oap-storage
include("oap-storage:oap-storage")
include("oap-storage:oap-storage-mongo")
include("oap-storage:oap-storage-mongo-test")
include("oap-storage:oap-storage-cloud")
include("oap-storage:oap-storage-cloud-aws-s3")
include("oap-storage:oap-storage-cloud-test")

// oap-mail
include("oap-mail:oap-mail")
include("oap-mail:oap-mail-mongo")
include("oap-mail:oap-mail-sendgrid")
include("oap-mail:oap-mail-test")

// oap-statsdb
include("oap-statsdb:oap-statsdb-common")
include("oap-statsdb:oap-statsdb-master")
include("oap-statsdb:oap-statsdb-node")
include("oap-statsdb:oap-statsdb-test")

// oap-ws
include("oap-ws:oap-ws")
include("oap-ws:oap-ws-api-api")
include("oap-ws:oap-ws-api-ws")
include("oap-ws:oap-ws-test")
include("oap-ws:oap-ws-admin-ws")
include("oap-ws:oap-ws-sso-api")
include("oap-ws:oap-ws-sso")
include("oap-ws:oap-ws-file-ws")
include("oap-ws:oap-ws-openapi")
include("oap-ws:oap-ws-openapi-ws")
include("oap-ws:oap-ws-openapi-maven-plugin")

// oap-maven-plugin (converted to Gradle plugins)
include("oap-maven-plugin:oap-maven")
include("oap-maven-plugin:oap-application-maven")
include("oap-maven-plugin:oap-dictionary-maven")
