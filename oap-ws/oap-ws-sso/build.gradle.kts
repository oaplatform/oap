plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-ws:oap-ws-sso-api"))
    api("de.taimos:totp:1.0")
}
