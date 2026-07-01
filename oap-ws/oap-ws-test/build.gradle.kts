plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-ws:oap-ws"))
    api(project(":oap-stdlib-test"))
    api(project(":oap-application:oap-application-test"))
}
