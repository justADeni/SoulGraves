package dev.faultyfunctions.soulgraves.listeners

import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.utils.Soul
import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import dev.faultyfunctions.soulgraves.api.event.SoulPreSpawnEvent
import dev.faultyfunctions.soulgraves.api.event.SoulSpawnEvent
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.utils.SpigotCompatUtils
import org.bukkit.*
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

		// CHECK PLAYER HAS PERMISSION
		if (ConfigManager.permissionRequired && !player.hasPermission("soulgraves.spawn")) return

		// CHECK TO MAKE SURE WE ARE IN AN ENABLED WORLD
		if (ConfigManager.disabledWorlds.contains(player.world.name)) return

		// CHECK PLAYER IS NOT KEEP INVENTORY
		if (e.keepInventory || player.world.getGameRuleValue(GameRule.KEEP_INVENTORY) == true) return

		// CHECK PLAYER HAVE ITEMS OR XP
		if (player.level == 0 && e.drops.isEmpty()) return

		// CALL EVENT
		val soulPreSpawnEvent = SoulPreSpawnEvent(player, e)
		Bukkit.getPluginManager().callEvent(soulPreSpawnEvent)
		if (soulPreSpawnEvent.isCancelled) return

		// SPAWN & DEFINE ENTITY
		val marker: Marker = player.world.spawnEntity(findSafeLocation(player.location), EntityType.MARKER) as Marker
		marker.isPersistent = true
		marker.isSilent = true
		marker.isInvulnerable = true

		// STORE SOUL KEY
		marker.persistentDataContainer.set(soulKey, DataType.BOOLEAN, true)

		// STORE PLAYER UUID
		marker.persistentDataContainer.set(soulOwnerKey, DataType.UUID, player.uniqueId)

		// STORE INVENTORY
		val inventory: MutableList<ItemStack?> = mutableListOf()
		e.drops.forEach items@ { item ->
			if (item != null) inventory.add(item)
		}
		marker.persistentDataContainer.set(soulInvKey, DataType.ITEM_STACK_ARRAY, inventory.toTypedArray())

		// STORE XP
		val xp: Int = SpigotCompatUtils.calculateTotalExperiencePoints(player.level)
		marker.persistentDataContainer.set(soulXpKey, DataType.INTEGER, xp)

		// STORE TIME LEFT
		val timeLeft = ConfigManager.timeStable + ConfigManager.timeUnstable
		marker.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, timeLeft)

		// CREATE SOUL DATA
		val soul = Soul(player.uniqueId, marker.uniqueId, marker.location, inventory, xp, timeLeft)
		SoulGraves.soulList.add(soul)
		Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable { MySQLDatabase.instance.saveSoul(soul, ConfigManager.serverName) })

		// CANCEL DROPS
		e.drops.clear()
		e.droppedExp = 0

		// CALL EVENT
		val soulSpawnEvent = SoulSpawnEvent(player, soul)
		Bukkit.getPluginManager().callEvent(soulSpawnEvent)
	}

	private fun findSafeLocation(locationToCheck: Location): Location {
		val safeLocation: Location = locationToCheck
		var block: Block = safeLocation.block

		// CHECK IF ABOVE THE MAX HEIGHT!!!
		val environment = locationToCheck.world!!.getEnvironment()
		while (block.type.isSolid || block.isLiquid || block.type == Material.VOID_AIR) {
			// NETHER
			if (environment == World.Environment.NETHER) {
				if (locationToCheck.y < 128 && safeLocation.y >= 127) {
					return safeLocation
				} else if (locationToCheck.y >= 128 && safeLocation.y >= 254) {
					return safeLocation
				}
			}

			// NORMAL
			if (safeLocation.y >= 319) return safeLocation
			safeLocation.add(0.0, 1.0, 0.0)
			block = safeLocation.block
		}

		return safeLocation
	}
}