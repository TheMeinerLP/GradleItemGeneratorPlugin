package dev.themeinerlp.itemgenerator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.themeinerlp.itemgenerator.models.Item
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

    @TaskAction
    fun generate() {
        val itemsPaths = itemsPaths.map { Paths.get(it.toString()).toFile() }.sorted().toSet()
        val items = itemsPaths.sorted().asSequence()
            .flatMap { it.walkTopDown().filter { file -> file.isFile } }
            .map { Json.decodeFromString<Item>(it.readText()) }
            .toSet()
        items.forEach {

            /*ItemStack.builder(Material.AIR).lore(it.lore.map { s ->
                Component.text(s)
            }.toList())
            list*/
            val arrayList = ClassName("kotlin.collections", "ArrayList")
            val component = ClassName("net.kyori.adventure.text", "Component")
            val arrayListOfLores = arrayList.parameterizedBy(component)

            val block = CodeBlock.builder()
            it.lore.forEach {
                block.addStatement("lore += %T.text(%S)", Component::class, it)
            }
            val buildItemFun = FunSpec.builder("buildItem")
                .returns(ItemStack::class)
                .addStatement("val lore = %T()", arrayListOfLores)
                .addCode(block.build())
                .addStatement("""val stack = %T.builder(%T.fromNamespaceId(%S) ?: throw %T("Material not found"))
.amount(%L)
.displayName(%T.text(%S))
.lore(lore)
.build()
                    """,
                    ItemStack::class,
                    Material::class,
                    it.material,
                    NullPointerException::class,
                    it.amount,
                    Component::class,
                    it.name)
                .addStatement("return stack")
                .build()
            val itemTypeSpec = TypeSpec.classBuilder(it.className)
                .addFunction(buildItemFun)
                .build()
            val kotlinFile = FileSpec.builder(it.packageName, it.fileName)
                .addType(itemTypeSpec)
                .build()
            kotlinFile.writeTo(getOuputDir().toPath())
        }
    }
}