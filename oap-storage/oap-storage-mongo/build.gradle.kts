plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api(project(":oap-storage:oap-storage"))
    api("org.mongodb:mongodb-driver-sync:5.4.0")
    api("org.apache.commons:commons-exec")
    api("io.mongock:mongock-standalone:5.5.1")
    api("io.mongock:mongodb-sync-v4-driver:5.5.1")

    testImplementation(project(":oap-stdlib-test"))
}
