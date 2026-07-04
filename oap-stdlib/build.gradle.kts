plugins {
    id("oap.java-convention")
    `java-library`
}

val jacksonVersion = "2.19.4"
val nettyVersion = "4.2.9.Final"

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api("io.micrometer:micrometer-core")
    api("org.apache.commons:commons-text")
    api("org.apache.commons:commons-compress")
    api("org.apache.commons:commons-lang3")
    api("commons-io:commons-io")
    api("commons-codec:commons-codec")
    api("joda-time:joda-time")
    api("org.slf4j:slf4j-api")
    api("ch.qos.logback:logback-classic")
    api("ch.qos.logback:logback-core")
    api("it.unimi.dsi:fastutil:8.5.12")
    api("org.quartz-scheduler:quartz:2.3.2")
    api("com.google.guava:guava")
    api("org.codehaus.plexus:plexus-utils:4.0.2")
    api("org.apache.commons:commons-configuration2")
    api("io.airlift:aircompressor:0.25")
    api("com.github.luben:zstd-jni:1.5.5-11")
    api("io.github.itning:guava-retrying3:3.0.3")
    api("com.jasonclawson:jackson-dataformat-hocon:1.1.0")
    api("de.undercouch:bson4jackson:2.18.0")
    api("oap:config:25.0.1")
    api("org.apache.commons:commons-collections4")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-joda:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-afterburner:$jacksonVersion")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    api("org.antlr:antlr4-runtime:4.13.0")
    api("de.ruedigermoeller:fst:3.0.4-jdk17")
    api("javax.activation:activation:1.1.1")
    api("org.openjdk.jol:jol-core:0.17")
    api("com.github.spotbugs:spotbugs-annotations:4.9.8")
    api("io.netty:netty-common:$nettyVersion")
    api("io.netty:netty-transport:$nettyVersion")
    api("io.netty:netty-buffer:$nettyVersion")
    api("io.netty:netty-handler:$nettyVersion")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.testng:testng")
}
