package dev.themeinerlp.itemgenerator

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

class ItemGeneratorPlugin : Plugin<Project> {

    companion object {
        const val GRADLE_GROUP = "Item Generator"
    }

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val generateJavaTaskProvider = project.tasks.register<ItemGenerateTask>("generateItems", ItemGenerateTask::class.java)
        generateJavaTaskProvider.configure { it.group = GRADLE_GROUP }

        project.getTasksByName("compileJava", false).forEach {
            it.dependsOn(generateJavaTaskProvider.get())
        }
        project.getTasksByName("compileKotlin", false).forEach {
            it.dependsOn(generateJavaTaskProvider.get())
        }
        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSets = javaConvention.sourceSets
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val outputDir = generateJavaTaskProvider.get().getOuputDir()
        mainSourceSet.java.srcDirs(outputDir)
    }

}