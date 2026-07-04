plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))

    api(project(":oap-http:oap-http"))
    api(project(":oap-application:oap-application-cli"))
    api(project(":oap-application:oap-application-annotation"))
}
