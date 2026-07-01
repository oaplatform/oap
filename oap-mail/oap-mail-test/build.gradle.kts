plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-mail:oap-mail"))
    api(project(":oap-mail:oap-mail-mongo"))
    api(project(":oap-stdlib-test"))
    api(project(":oap-storage:oap-storage-mongo-test"))
}
