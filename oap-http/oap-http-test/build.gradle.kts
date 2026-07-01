plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-http:oap-http"))
    api(project(":oap-stdlib-test"))

    testImplementation("org.mock-server:mockserver-netty:5.15.0")
}
