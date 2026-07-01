plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-stdlib"))
    api(project(":oap-formats:oap-template"))
    api(project(":oap-message:oap-message-common"))
    api(project(":oap-formats:oap-tsv:oap-tsv"))
    api(project(":oap-storage:oap-storage-cloud"))
    api("org.apache.velocity:velocity-engine-core")
}
