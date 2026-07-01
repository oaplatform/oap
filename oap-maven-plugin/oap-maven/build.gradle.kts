plugins {
    `java-gradle-plugin`
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))
}

gradlePlugin {
    plugins {
        create("oapCopy") {
            id = "oap.copy"
            implementationClass = "oap.maven.OapCopyPlugin"
        }
    }
}
