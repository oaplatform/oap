plugins {
    id("oap.java-convention")
    `java-library`
}

sourceSets {
    main {
        java {
            srcDir("src/main/java-antlr-generated")
        }
    }
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))
    api(project(":oap-stdlib"))
    api("org.antlr:antlr4-runtime:4.13.0")
    testImplementation(project(":oap-stdlib-test"))
}
