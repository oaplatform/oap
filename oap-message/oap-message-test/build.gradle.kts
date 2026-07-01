plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-message:oap-message-client"))
    api(project(":oap-message:oap-message-server"))
    api(project(":oap-stdlib-test"))
    api(project(":oap-application:oap-application-test"))
}
