plugins {
    id("oap.java-convention")
    `java-library`
}

val jettyVersion = "12.1.6"

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api("io.undertow:undertow-core:2.3.23.Final")
    api("org.apache.httpcomponents:httpcore:4.4.16")
    api("org.apache.httpcomponents:httpclient:4.5.14")
    api("org.apache.httpcomponents:httpcore-nio:4.4.16")
    api("org.apache.httpcomponents:httpasyncclient:4.1.5")
    api("org.apache.httpcomponents:httpmime:4.5.14")
    api("org.eclipse.jetty:jetty-client:$jettyVersion")
    api("org.eclipse.jetty.compression:jetty-compression-gzip:$jettyVersion")
    api("org.eclipse.jetty.compression:jetty-compression-brotli:$jettyVersion")
    api("org.eclipse.jetty.compression:jetty-compression-zstandard:$jettyVersion")
    api("dnsjava:dnsjava:3.6.4")

    testImplementation(project(":oap-stdlib-test"))
}
