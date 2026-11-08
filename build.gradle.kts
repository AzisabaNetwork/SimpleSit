plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "net.azisaba"
version = "1.0.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
}

val targetJavaVersion = 8
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.shadowJar {
    archiveClassifier.set("")
    // 他プラグインが同梱する Kotlin と衝突しないようにリロケート
    relocate("kotlin", "net.azisaba.simplesit.libs.kotlin")
    relocate("org.jetbrains.annotations", "net.azisaba.simplesit.libs.org.jetbrains.annotations")
    relocate("org.intellij.lang.annotations", "net.azisaba.simplesit.libs.org.intellij.lang.annotations")
    minimize()
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
