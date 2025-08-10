package dev.faultyfunctions.soulgraves.compatibilities

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.event.SoulPreSpawnEvent
import dev.faultyfunctions.soulgraves.api.event.SoulSpawnEvent
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class VaneEnchantmentsHook : Listener {
	companion object {
		val instance: VaneEnchantmentsHook by lazy { VaneEnchantmentsHook() }
	}

	fun init() {
		SoulGraves.plugin.server.pluginManager.registerEvents(VaneEnchantmentsHook(), SoulGraves.plugin)
		SoulGraves.plugin.logger.info("[√] Vane Enchantments hook loaded!")
	}

	// Make sure we don't spawn a soul if the player only has soulbound items in their inventory and no xp
	@EventHandler
	fun onSoulPreSpawn(e: SoulPreSpawnEvent) {
		if (e.deathEvent.entity.level != 0 && ConfigManager.soulsStoreXP) {
			return
		}
		for (item in e.deathEvent.entity.inventory.contents) {
			if (item != null && item.enchantments.filter { it.key.key.toString() == "vane_enchantments:soulbound" }.isEmpty()) {
				return
			}
		}
		e.isCancelled = true
	}

	// Make sure soulbound items aren't dropped on death
	@EventHandler
	fun onSoulSpawn(e: SoulSpawnEvent) {
		// STORE SOULBOUND ITEMS
		val soulboundInventory: MutableList<ItemStack?> = mutableListOf()
		e.soul.inventory.forEachIndexed { index, item ->
			if (item != null) {
				if (item.enchantments.filter { it.key.key.toString() == "vane_enchantments:soulbound" }.isNotEmpty()) {
					e.soul.inventory[index] = null
					soulboundInventory.add(item)
				} else {
					soulboundInventory.add(null)
				}
			} else {
				soulboundInventory.add(null)
			}
		}

		// RESTORE SOULBOUND ITEMS 1 TICK LATER
		if (soulboundInventory.isNotEmpty()) {
			Bukkit.getScheduler().runTaskLater(SoulGraves.plugin, Runnable {
				soulboundInventory.forEachIndexed { index, item ->
					if (item != null) {
						e.player.inventory.setItem(index, item)
					}
				}
			}, 1L)
		}
	}
}