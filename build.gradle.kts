import com.github.jengelman.gradle.plugins.shadow.ShadowApplicationPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayPlugin
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.jfrog.bintray.gradle.BintrayExtension

val kotlinVersion = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion
val kotlinxCoroutinesVersion = "0.22.2"

project.group = "de.swirtz"
project.version = "0.0.1"

plugins {
    kotlin("jvm") version "1.2.30"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "2.0.2"
}


kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    compile(kotlin("stdlib-jre8", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    compile("org.slf4j:slf4j-api:1.7.14")
    compile("ch.qos.logback:logback-classic:1.1.3")

    testCompile(kotlin("test-junit", kotlinVersion))
    testCompile("junit:junit:4.11")
    testCompile("org.eclipse.jetty:jetty-server:9.4.7.v20170914")
}

repositories {
    mavenCentral()
    jcenter()
}
val shadowJar: ShadowJar by tasks
shadowJar.apply {
    classifier = null
}

tasks {
    withType(GradleBuild::class.java) {
        dependsOn(shadowJar)
    }
    withType(Test::class.java) {
        testLogging.showStandardStreams = true
    }

    withType<GenerateMavenPom> {
        destination = file("$buildDir/libs/${shadowJar.archiveName}.pom")
    }
}

val publicationName = "tlslib"
publishing {
    publications.invoke {
        publicationName(MavenPublication::class) {
            artifact(shadowJar)
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                //Iterate over the compile dependencies (we don't want the test ones), adding a <dependency> node for each
                configurations.compile.allDependencies.forEach {
                    dependenciesNode.appendNode("dependency").apply {
                        appendNode("groupId", it.group)
                        appendNode("artifactId", it.name)
                        appendNode("version", it.version)
                    }
                }
            }
        }
    }
}

bintray {
    val bintrayUser = project.findProperty("bintrayUser") as String?
    val bintrayApiKey = project.findProperty("bintrayApiKey") as String?
    user = "s1monw1"
    key = "b480d54109a1e55e5ea88a8d9a3defe6eea63325"
    publish = true
    setPublications(publicationName)
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "SeKurity"
        name = project.name
        userOrg = "simon-wirtz"
        description = "Simple Lib for TLS/SSL socket handling written in Kotlin"
        setLabels("kotlin")
        setLicenses("MIT")
        desc = project.description
    })
}