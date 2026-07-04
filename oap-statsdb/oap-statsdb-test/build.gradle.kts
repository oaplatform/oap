plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-statsdb:oap-statsdb-master"))
    api(project(":oap-statsdb:oap-statsdb-node"))
    api(project(":oap-storage:oap-storage-mongo-test"))
    api(project(":oap-message:oap-message-test"))
}
