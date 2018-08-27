import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.2.60"
val kotlinTestVersion = "3.1.9"
val vertxUnitVersion = "3.5.3"
val vertxVersion = "3.5.3"
val jacksonVersion = "2.9.6"
val slf4jVersion = "1.7.25"
val mockkVersion = "1.8.6"
val mainVerticalName = "com.fueledbysoda.todo.MainVerticle"

group = "com.fueledbysoda.todo"
version = "0.0.1"

repositories {
    jcenter()
    mavenCentral()
}

plugins {
    java
    application
    kotlin("jvm") version "1.2.60"
    id("com.github.johnrengelman.shadow") version "2.0.4"
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-mysql-postgresql-client:$vertxVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlinTestVersion")
    testImplementation("io.vertx:vertx-unit:$vertxUnitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

application {
    mainClassName = "io.vertx.core.Launcher"
}

val shadowJar = tasks.withType<ShadowJar> {
    classifier = "fat"
    mergeServiceFiles {
        include("META-INF/services/io.vertx.core.spi.VerticleFactory")
    }
    manifest {
        attributes(mapOf("Main-Class" to application.mainClassName, "Main-Verticle" to mainVerticalName))
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks{
    withType<JavaExec> {
        args("run", mainVerticalName, "--launcher-class=${application.mainClassName}")
    }

    "stage" {
        dependsOn(shadowJar)
    }

    "build"{
        dependsOn(shadowJar)
    }
}