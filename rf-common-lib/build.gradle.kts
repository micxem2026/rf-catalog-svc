import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`       // нужен компонент components["java"]
    `maven-publish`      // даёт publishing { ... }
}

kotlin {
    jvmToolchain(17)
}

repositories {
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

version = "1.0.5"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            // Этот блок будет выполнен после полной конфигурации проекта
            project.afterEvaluate {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }

            pom {
                name.set(project.name)
                description.set(project.description)

                developers {
                    developer {
                        id.set("micxem")
                        name.set("MicXEm")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitLab"
            url = uri(project.findProperty("gitlab.registry.url") ?: "")

            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = project.findProperty("gitlab.registry.token")?.toString() ?: ""
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
            isAllowInsecureProtocol = project.findProperty("gitlab.allow.insecure") == "true"
        }
    }
}

dependencies {

    implementation(platform("org.springdoc:springdoc-openapi-bom:${property("springDocVersion")}"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web:${property("springBootVersion")}")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${property("springBootVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:${property("springBootVersion")}")


    // Database
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

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