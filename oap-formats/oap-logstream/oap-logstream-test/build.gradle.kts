plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-formats:oap-logstream:oap-logstream-net-server"))
    api(project(":oap-formats:oap-logstream:oap-logstream-data"))
    api(project(":oap-formats:oap-logstream:oap-logstream-net-client"))
    api(project(":oap-stdlib"))
    api(project(":oap-stdlib-test"))
    api(project(":oap-formats:oap-template-test"))
    api("org.apache.commons:commons-csv")
    api(project(":oap-storage:oap-storage-cloud-test"))
    testImplementation("com.clickhouse:client-v2:0.8.3")
}
