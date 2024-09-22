package com.cobbleton.soulgraves.listeners

import com.cobbleton.soulgraves.*
import com.cobbleton.soulgraves.utils.Soul
import com.jeff_media.morepersistentdatatypes.DataType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Marker
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack

class PlayerDeathListener() : Listener {
	@EventHandler(priority = EventPriority.NORMAL)
	fun onPlayerDeathEvent(e: PlayerDeathEvent) {
		// SPAWN & DEFINE ENTITY
		val entity: Marker = e.player.world.spawnEntity(findSafeLocation(e.player.location), EntityType.MARKER) as Marker
		entity.isPersistent = true
		entity.isSilent = true
		entity.isInvulnerable = true
		entity.isInvisible = true

		// STORE CHUNK LOCATION IN WORLD'S PERSISTENT DATA CONTAINER FOR LOADING LATER
		val chunkList: MutableList<Long>? = entity.world.persistentDataContainer.get(soulChunksKey, DataType.asList(DataType.LONG))
		if (chunkList != null && !chunkList.contains(entity.chunk.chunkKey)) {
			chunkList.add(entity.chunk.chunkKey)
			entity.world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
		}

		// TAG ENTITY WITH SOUL KEY
		entity.persistentDataContainer.set(soulKey, DataType.BOOLEAN, true)

		// STORE PLAYER UUID
		entity.persistentDataContainer.set(soulOwnerKey, DataType.UUID, e.player.uniqueId)

		// CREATE INVENTORY
		val inventory: MutableList<ItemStack?> = mutableListOf()
		e.player.inventory.forEach { item ->
			if (item != null) {
				inventory.add(item)
			} else {
				inventory.add(null)
			}
		}
		entity.persistentDataContainer.set(soulInvKey, DataType.ITEM_STACK_ARRAY, inventory.toTypedArray())

		// MANAGE XP
		val xp: Int = e.player.calculateTotalExperiencePoints()
		entity.persistentDataContainer.set(soulXpKey, DataType.INTEGER, xp)

		// MANAGE TIME LEFT
		var timeLeft = 300
		entity.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, timeLeft)

		// CREATE SOUL DATA
		val soul = Soul(e.player.uniqueId, entity.uniqueId, entity.location, inventory, xp, timeLeft)
		SoulGraves.soulList.add(soul)

		// CANCEL DROPS
		e.drops.clear()
		e.droppedExp = 0
	}

	private fun findSafeLocation(locationToCheck: Location): Location {
		val safeLocation: Location = locationToCheck
		var block: Block = safeLocation.block

		while (block.isSolid || block.isLiquid || block.type == Material.VOID_AIR) {
			safeLocation.add(0.0, 1.0, 0.0)
			block = safeLocation.block
		}

		return safeLocation
	}
}