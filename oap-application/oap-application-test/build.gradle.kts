plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-application:oap-application"))
    api(project(":oap-stdlib-test"))
    api(project(":oap-http:oap-http-test"))
}
