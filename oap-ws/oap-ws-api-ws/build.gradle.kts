plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-stdlib"))
    api(project(":oap-ws:oap-ws"))
    api(project(":oap-ws:oap-ws-api-api"))
    api(project(":oap-ws:oap-ws-sso-api"))
    testImplementation(project(":oap-ws:oap-ws-test"))
}
