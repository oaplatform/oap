plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-storage:oap-storage-mongo"))
    api(project(":oap-stdlib-test"))
    api("de.bwaldvogel:mongo-java-server:1.47.0")
}
