package dev.themeinerlp.itemgenerator

import org.gradle.api.provider.Property

interface ItemGeneratorPluginExtension {
    val itemModelPath: Property<String>
}