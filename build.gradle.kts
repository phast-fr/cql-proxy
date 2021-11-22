import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ossrhUsername: String by project
val ossrhPassword: String by project

plugins {
    id("org.springframework.boot") version "2.6.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.spring") version "1.6.0"
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "fr.phast"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
}

extra["springBootAdminVersion"] = "2.4.3"
extra["springCloudVersion"] = "2021.0.0-RC1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-sleuth-zipkin")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("info.cqframework:cql-to-elm:1.5.4")
    implementation("org.opencds.cqf.cql:engine:1.5.2")

    implementation("fr.phast:phast-fhir-kt:0.0.10-SNAPSHOT")
    implementation("fr.phast:cql-engine-fhir:0.0.10-SNAPSHOT")
    implementation("fr.phast:cql-services:0.0.17-SNAPSHOT")
}

dependencyManagement {
    imports {
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            afterEvaluate {
                artifactId = tasks.jar.get().archiveBaseName.get()
            }

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set(rootProject.name)
                packaging = "jar"
                description.set(project.description)
                url.set("https://github.com/phast-fr/cql-proxy")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("davidouagne")
                        name.set("David Ouagne")
                        email.set("david.ouagne@phast.fr")
                        organization.set("Phast")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/phast-fr/cql-proxy.git")
                    developerConnection.set("scm:git:https://github.com/phast-fr/cql-proxy.git")
                    url.set("https://github.com/phast-fr/cql-proxy.git")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
