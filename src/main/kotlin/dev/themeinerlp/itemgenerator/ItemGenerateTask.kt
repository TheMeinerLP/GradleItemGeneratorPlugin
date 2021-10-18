package dev.themeinerlp.itemgenerator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.themeinerlp.itemgenerator.models.Item
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Paths

open class ItemGenerateTask : DefaultTask() {

    @Input
    var generatedSourcesDir: String = project.buildDir.absolutePath

    @Input
    var itemsPaths = mutableListOf<Any>("${project.projectDir}/src/main/resources/items")

    @OutputDirectory
    fun getOuputDir(): File {
        return Paths.get("$generatedSourcesDir/generated").toFile()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun generate() {
        this.getOuputDir().deleteRecursively()
        val itemsPaths = itemsPaths.map { Paths.get(it.toString()).toFile() }.sorted().toSet()
        val items = itemsPaths.sorted().asSequence()
            .flatMap { it.walkTopDown().filter { file -> file.isFile } }
            .map { Json{
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = false
            }.decodeFromString<Item>(it.readText()) }
            .toSet()
        items.forEach {
            val buildItemFun = FunSpec.builder("getItem")
                .returns(ItemStack::class)
                .addStatement("return this.stack")
                .build()
            val itemTypeSpec = TypeSpec.classBuilder(it.className)
                itemTypeSpec.addProperty(buildName(it))
                itemTypeSpec.addProperty(buildLore(it))
                itemTypeSpec.addProperty(buildMaterial(it))
                itemTypeSpec.addProperty(buildAmount(it))
                if (it.meta != null) buildMetaDataValues(it).forEach { itemTypeSpec.addProperty(it)  }
                itemTypeSpec.addProperty(buildItemStack(it))


            itemTypeSpec.addFunction(buildItemFun)
            val kotlinFile = FileSpec.builder(it.packageName, it.fileName)
                .addType(itemTypeSpec.build())
                .build()
            kotlinFile.writeTo(getOuputDir().toPath())
        }
    }
    private fun buildMetaDataValues(item: Item): List<PropertySpec> {
        val properties = mutableListOf<PropertySpec>()
        val itemMeta = item.meta
        if (itemMeta?.customModelData != null) {
            val property = PropertySpec.builder("customModelData", Int::class, KModifier.PRIVATE)
            property.initializer("%L", itemMeta.customModelData)
            properties.add(property.build())
        }
        return properties
    }

    private fun buildMeta(item: Item): CodeBlock {
        val itemMeta = item.meta
        val block = CodeBlock.builder()
        block.add("\n.meta { builder -> \n")
        if (itemMeta?.customModelData != null) block.add("builder.customModelData(this.customModelData)")
        block.add(" }")
        return block.build()
    }

    private fun buildItemStack(item: Item): PropertySpec {
        val property = PropertySpec.builder("stack", ItemStack::class, KModifier.PRIVATE)
        val block = CodeBlock.builder()
        block.add("%T.builder(this.material)", ItemStack::class)
        block.add(".amount(this.amount)")
        block.add(".displayName(this.name)")
        block.add(".lore(this.lore)")
        if (item.meta != null) {
            block.add(buildMeta(item))
        }
        block.add(".build()")

        property.initializer(block.build())
        return property.build()
    }

    private fun buildAmount(item: Item): PropertySpec {
        val property = PropertySpec.builder("amount", Int::class, KModifier.PRIVATE)
        property.initializer("%L", item.amount)
        return property.build()
    }

    private fun buildMaterial(item: Item): PropertySpec {
        val property = PropertySpec.builder("material", Material::class, KModifier.PRIVATE)
        property.initializer("%T.fromNamespaceId(%S) ?: throw %T(\"Material not found\")", Material::class, item.material, NullPointerException::class)
        return property.build()
    }

    private fun buildName(item: Item): PropertySpec {
        val property = PropertySpec.builder("name", Component::class, KModifier.PRIVATE)
        property.initializer("%T.text(%S)", Component::class, item.name)
        return property.build()
    }

    private fun buildLore(item: Item): PropertySpec {
        val block = CodeBlock.builder()
        block.add("listOf(")
        item.lore.forEachIndexed { index, s ->
            if (index == (item.lore.size-1)) {
                block.add("%T.text(%S))\n", Component::class, s)
            } else {
                block.add("%T.text(%S), ", Component::class, s)
            }

        }
        val property = PropertySpec.builder("lore", List::class.parameterizedBy(Component::class), KModifier.PRIVATE)
        property.initializer(block.build())
        return property.build()
    }
}