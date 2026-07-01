plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-application:oap-application-annotation"))
    api(project(":oap-ws:oap-ws-sso"))
    api("io.modelcontextprotocol.sdk:mcp-core:1.1.3")
    api("io.modelcontextprotocol.sdk:mcp-json-jackson3:1.1.3")
}
