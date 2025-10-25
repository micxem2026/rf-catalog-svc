import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(platform("org.springdoc:springdoc-openapi-bom:${property("springDocVersion")}"))

    implementation(project(":rf-common-lib"))
    implementation(project(":rf-contract-client"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${property("springBootVersion")}")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:${property("springBootVersion")}")

    // Database
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Swagger/OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    // Fix vulnerability
    implementation("ch.qos.logback:logback-classic:1.5.19") {
        because("CVE-2025-11226 - Security fix")
    }
    implementation("org.apache.commons:commons-lang3:3.18.0") {
        because("CVE-2025-48924 - Security fix")
    }

}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


