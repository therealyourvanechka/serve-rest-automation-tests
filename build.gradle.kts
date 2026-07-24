plugins {
    id("java")
    id("io.qameta.allure") version "4.0.2"
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

allure {
    version = "2.42.0"
    report {
        reportDir.set(layout.buildDirectory)
    }
}

dependencies {
    implementation("io.rest-assured:rest-assured:5.5.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.assertj:assertj-core:3.26.3")

    testImplementation("io.rest-assured:json-schema-validator:5.5.0")

    implementation("io.qameta.allure:allure-java-commons:2.29.1")
    testImplementation("io.qameta.allure:allure-rest-assured:2.29.1")

    testImplementation("net.datafaker:datafaker:2.4.3")

    testImplementation("io.jsonwebtoken:jjwt:0.12.6")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        delete("build/allure-results")
    }
}
