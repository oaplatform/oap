plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api(project(":oap-ws:oap-ws"))
    api("com.auth0:java-jwt:4.4.0") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }
    api("com.auth0:jwks-rsa:0.22.0") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }

    testImplementation(project(":oap-ws:oap-ws-test"))
}
