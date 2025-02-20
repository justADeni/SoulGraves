package dev.faultyfunctions.soulgraves.database

import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import dev.faultyfunctions.soulgraves.utils.Soul
import dev.faultyfunctions.soulgraves.utils.SpigotCompatUtils
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Marker
import org.bukkit.inventory.ItemStack

val soulChunksKey = NamespacedKey(SoulGraves.plugin, "soul-chunks")
val soulKey = NamespacedKey(SoulGraves.plugin, "soul")
val soulOwnerKey = NamespacedKey(SoulGraves.plugin, "soul-owner")
val soulInvKey = NamespacedKey(SoulGraves.plugin, "soul-inv")
val soulXpKey = NamespacedKey(SoulGraves.plugin, "soul-xp")
val soulDeathTimeKey = NamespacedKey(SoulGraves.plugin, "soul-death-time")
val soulExpireTimeKey = NamespacedKey(SoulGraves.plugin, "soul-expire-time")

class PDCDatabase private constructor() {

    companion object {
        val instance by lazy { PDCDatabase().apply { initSouls() } }
    }

    private fun initSouls() {
        for (world in Bukkit.getWorlds()) {
            if (!world.persistentDataContainer.has(soulChunksKey)) {
                world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), mutableListOf<Long>())
            } else {
                // GET CHUNK KEY LIST AND MAKE SURE THEY ARE UNIQUE
                val chunkKeyList: List<Long>? = world.persistentDataContainer.get(
                    soulChunksKey, DataType.asList(
                        DataType.LONG))?.distinct()
                // RESET CHUNK LIST IN WORLD PDC
                world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), mutableListOf<Long>())

                chunkKeyList?.forEach chunkKeyLoop@{ chunkKey ->
                    SpigotCompatUtils.getChunkAt(chunkKey, world).entities
                        .filterIsInstance<Marker>()
                        .filter { it.persistentDataContainer.has(soulKey) }
                        .forEach markerEntityLoop@{
                            // Get Data
                            val ownerUUID = it.persistentDataContainer.get(soulOwnerKey, DataType.UUID)!!
                            val inventory = (it.persistentDataContainer.get(soulInvKey, DataType.ITEM_STACK_ARRAY))?.toMutableList() ?: mutableListOf<ItemStack?>()
                            val xp: Int = it.persistentDataContainer.get(soulXpKey, DataType.INTEGER) ?: 0
                            val deathTime: Long = it.persistentDataContainer.get(soulDeathTimeKey, DataType.LONG) ?: 0
                            val expireTime: Long = it.persistentDataContainer.get(soulExpireTimeKey, DataType.LONG) ?: 0

                            // Init Souls
                            Soul.initAndStart(
                                markerUUID = it.uniqueId,
                                ownerUUID = ownerUUID,
                                location = it.location,
                                inventory = inventory,
                                xp = xp,

                                deathTime = deathTime,
                                expireTime = expireTime
                            )
                        }
                }
            }
        }
    }
}