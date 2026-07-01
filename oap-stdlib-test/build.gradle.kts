plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api("oap:oap-teamcity:25.0.0")
    api("org.testng:testng")
    api("org.assertj:assertj-core")
    api("net.bytebuddy:byte-buddy:1.17.6")
    api("org.mockito:mockito-core:5.18.0")
    api("org.testcontainers:testcontainers:2.0.5")
}
