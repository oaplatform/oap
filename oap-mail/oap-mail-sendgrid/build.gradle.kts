plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-mail:oap-mail"))
    api("com.sendgrid:sendgrid-java:4.9.3") {
        exclude(group = "org.objenesis", module = "objenesis")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-annotations")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }
}
