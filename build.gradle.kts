import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ossrhUsername: String by project
val ossrhPassword: String by project

plugins {
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
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
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.5.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:2.5.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.5")

    implementation("org.springframework.boot:spring-boot-starter-actuator:2.5.5")
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.5.5")

    implementation("info.cqframework:cql-to-elm:1.5.4")
    implementation("org.opencds.cqf.cql:engine:1.5.2")

    implementation("fr.phast:phast-fhir-kt:0.0.3-SNAPSHOT")
    implementation("fr.phast:cql-engine-fhir:0.0.3-SNAPSHOT")
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
                url.set("https://github.com/phast-fr/TODO")
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
                    connection.set("scm:git:https://github.com/phast-fr/TODO.git")
                    developerConnection.set("scm:git:https://github.com/phast-fr/TODO.git")
                    url.set("https://github.com/phast-fr/TODO.git")
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
