plugins {
    id("java")
}

group = "com.serverest"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // REST Assured + Jackson
    implementation("io.rest-assured:rest-assured:5.5.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")


    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.26.3")

    // JSON Schema Validation
    testImplementation("io.rest-assured:json-schema-validator:5.5.0")

    // Allure
    implementation("io.qameta.allure:allure-java-commons:2.29.1")
    testImplementation("io.qameta.allure:allure-rest-assured:2.29.1")

    // DataFaker
    testImplementation("net.datafaker:datafaker:2.4.3")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
}

tasks.test {
    useJUnitPlatform()
}