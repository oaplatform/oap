plugins {
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-stdlib"))

    testImplementation(gradleApi())
    testImplementation(project(":oap-stdlib-test"))
}

gradlePlugin {
    plugins {
        create("dictionary") {
            id = "oap.dictionary"
            implementationClass = "oap.dictionary.gradle.DictionaryPlugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}