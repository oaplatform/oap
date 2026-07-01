plugins {
    `java-gradle-plugin`
    `maven-publish`
    `java-library`
}

gradlePlugin {
    plugins {
        create("startupScripts") {
            id = "oap.startup-scripts"
            implementationClass = "oap.application.maven.StartupScriptsPlugin"
        }
    }
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))

}