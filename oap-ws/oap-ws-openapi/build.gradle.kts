plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-stdlib"))
    api(project(":oap-ws:oap-ws"))
    api(project(":oap-ws:oap-ws-api-api"))
    api(project(":oap-ws:oap-ws-sso"))
    testImplementation(project(":oap-ws:oap-ws-test"))
    api("io.swagger.core.v3:swagger-core-jakarta:2.2.40")
}
