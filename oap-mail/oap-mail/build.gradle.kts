plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
    api("com.sun.mail:javax.mail:1.6.2") {
        exclude(group = "javax.activation", module = "activation")
    }
    api("commons-codec:commons-codec")
    api("org.apache.velocity:velocity-engine-core")
    api("javax.activation:activation:1.1.1")
}
