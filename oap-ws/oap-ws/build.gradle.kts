plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api(project(":oap-formats:oap-json:oap-json-schema"))
    api(project(":oap-application:oap-application"))
    api("org.javassist:javassist:3.29.2-GA")
}
