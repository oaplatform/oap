plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api("net.openhft:affinity:3.23.3")

    testImplementation(project(":oap-stdlib-test"))
}
