plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api(project(":oap-http:oap-http"))
    api("io.micrometer:micrometer-core")
    api("io.micrometer:micrometer-registry-prometheus")

    testImplementation(project(":oap-stdlib-test"))
    testImplementation(project(":oap-http:oap-http-test"))
}
