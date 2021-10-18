package dev.themeinerlp.itemgenerator.models

import kotlinx.serialization.Serializable

@Serializable
data class ItemMeta(
    val customModelData: Int?,
    val damage: Int?,
)
