plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-formats:oap-logstream:oap-logstream-data"))
    api(project(":oap-formats:oap-template"))
    testImplementation(project(":oap-stdlib-test"))
}
