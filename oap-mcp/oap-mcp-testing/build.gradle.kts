plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-mcp:oap-mcp"))
    api(project(":oap-mcp:oap-mcp-admin"))
    api("io.modelcontextprotocol.sdk:mcp-test:1.1.3")

    testImplementation(project(":oap-application:oap-application-test"))
    testImplementation(project(":oap-ws:oap-ws-sso-api"))
}
