// subproject configuration is handled by oap.java-convention

subprojects {
    plugins.withType<JavaPlugin> {
        configurations {
            named("testAnnotationProcessor") {
                extendsFrom(configurations["annotationProcessor"])
            }
            named("testCompileOnly") {
                extendsFrom(configurations["compileOnly"])
            }
        }
    }
    tasks.withType<Checkstyle>().configureEach {
        exclude { it.file.path.contains("java-antlr-generated") }
    }
}
