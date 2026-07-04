plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api("org.openjdk.jol:jol-core:0.17")

    testImplementation(project(":oap-stdlib-test"))
    testImplementation(project(":oap-http:oap-http-test"))
    testImplementation(project(":oap-application:oap-application-test"))
}
