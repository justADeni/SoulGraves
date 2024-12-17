package dev.faultyfunctions.soulgraves.listeners

import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.utils.Soul
import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack

class PlayerDeathListener() : Listener {
	@EventHandler(priority = EventPriority.NORMAL)
	fun onPlayerDeathEvent(e: PlayerDeathEvent) {
		// READABILITY VARIABLES
		val player: Player = e.entity
		
		// CHECK TO MAKE SURE WE ARE IN AN ENABLED WORLD
		if (ConfigManager.disabledWorlds.contains(player.world.name)) {
			return
		}

		// SPAWN & DEFINE ENTITY
		val marker: Marker = player.world.spawnEntity(findSafeLocation(player.location), EntityType.MARKER) as Marker
		marker.isPersistent = true
		marker.isSilent = true
		marker.isInvulnerable = true

		// STORE CHUNK LOCATION IN WORLD'S PERSISTENT DATA CONTAINER FOR LOADING LATER
		val chunkList: MutableList<Long>? = marker.world.persistentDataContainer.get(soulChunksKey, DataType.asList(DataType.LONG))
		if (chunkList != null && !chunkList.contains(SoulGraves.compat.getChunkKey(marker.location.chunk))) {
			chunkList.add(SoulGraves.compat.getChunkKey(marker.location.chunk))
			marker.world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
		}

		// TAG ENTITY WITH SOUL KEY
		marker.persistentDataContainer.set(soulKey, DataType.BOOLEAN, true)

		// STORE PLAYER UUID
		marker.persistentDataContainer.set(soulOwnerKey, DataType.UUID, player.uniqueId)

		// CREATE INVENTORY
		val inventory: MutableList<ItemStack?> = mutableListOf()
		val soulboundInventory: MutableList<ItemStack?> = mutableListOf()
		player.inventory.forEach items@ { item ->
			if (item != null) {
				// SKIP IF ITEM IS SOULBOUND
				item.enchantments.forEach { enchantment ->
					if (enchantment.key.key.toString() == "vane_enchantments:soulbound") {
						soulboundInventory.add(item)
						inventory.add(null)
						return@items
					}
				}
				inventory.add(item)
			} else {
				inventory.add(null)
			}
		}
		marker.persistentDataContainer.set(soulInvKey, DataType.ITEM_STACK_ARRAY, inventory.toTypedArray())

		// MANAGE XP
		val xp: Int = player.totalExperience
		marker.persistentDataContainer.set(soulXpKey, DataType.INTEGER, xp)

		// MANAGE TIME LEFT
		val timeLeft = ConfigManager.timeStable + ConfigManager.timeUnstable
		marker.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, timeLeft)

		// CREATE SOUL DATA
		val soul = Soul(player.uniqueId, marker.uniqueId, marker.location, inventory, xp, timeLeft)
		SoulGraves.soulList.add(soul)

		// CANCEL DROPS
		e.drops.clear()
		e.drops.addAll(soulboundInventory)
		e.droppedExp = 0
	}

	private fun findSafeLocation(locationToCheck: Location): Location {
		val safeLocation: Location = locationToCheck
		var block: Block = safeLocation.block

		while (block.type.isSolid || block.isLiquid || block.type == Material.VOID_AIR) {
			safeLocation.add(0.0, 1.0, 0.0)
			block = safeLocation.block
		}

		return safeLocation
	}
}