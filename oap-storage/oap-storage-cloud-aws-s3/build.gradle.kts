plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-storage:oap-storage-cloud"))
    api("software.amazon.awssdk:s3")
    api("software.amazon.awssdk.crt:aws-crt")
    api("software.amazon.awssdk:s3-transfer-manager") {
        exclude(group = "software.amazon.awssdk", module = "s3")
        exclude(group = "software.amazon.awssdk", module = "sdk-core")
        exclude(group = "software.amazon.awssdk", module = "utils")
        exclude(group = "software.amazon.awssdk", module = "annotations")
        exclude(group = "software.amazon.awssdk", module = "regions")
        exclude(group = "software.amazon.awssdk.crt", module = "aws-crt")
    }
}
