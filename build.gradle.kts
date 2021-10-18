plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.5.10"
    `maven-publish`
    kotlin("plugin.serialization") version "1.5.31"
}

group = "dev.themeinerlp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://repo.spongepowered.org/maven")
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("com.squareup:kotlinpoet:1.10.1")
    implementation("com.github.Minestom:Minestom:-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("ItemGenerator") {
            id = "dev.themeinerlp.itemgenerator"
            implementationClass = "dev.themeinerlp.itemgenerator.ItemGeneratorPlugin"
        }
    }
}