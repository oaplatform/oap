plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-statsdb:oap-statsdb-common"))
    api(project(":oap-stdlib"))
    api(project(":oap-message:oap-message-server"))
    api(project(":oap-storage:oap-storage-mongo"))
}
