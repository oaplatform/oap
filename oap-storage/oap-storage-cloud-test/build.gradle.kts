plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib-test"))
    api(project(":oap-storage:oap-storage-cloud"))
    api(project(":oap-storage:oap-storage-cloud-aws-s3"))
    api("org.testcontainers:testcontainers:2.0.5")
    api("org.testcontainers:localstack:1.21.4") {
        exclude(group = "org.testcontainers", module = "testcontainers")
    }
}
