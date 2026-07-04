plugins {
    `java-gradle-plugin`
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    implementation(project(":oap-ws:oap-ws-openapi"))
    implementation(project(":oap-ws:oap-ws"))
    implementation(project(":oap-application:oap-application"))
}

gradlePlugin {
    plugins {
        create("openapi") {
            id = "oap.openapi"
            implementationClass = "oap.openapi.gradle.OpenApiPlugin"
        }
    }
}
