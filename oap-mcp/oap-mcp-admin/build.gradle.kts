plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-mcp:oap-mcp"))
    api(project(":oap-ws:oap-ws-admin-ws"))
}
