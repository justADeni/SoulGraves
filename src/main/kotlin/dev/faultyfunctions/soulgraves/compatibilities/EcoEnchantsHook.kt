package dev.faultyfunctions.soulgraves.compatibilities

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.event.SoulSpawnEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class EcoEnchantsHook : Listener {
	companion object {
		val instance: EcoEnchantsHook by lazy { EcoEnchantsHook() }
	}

	fun registerEvents() {
		SoulGraves.plugin.server.pluginManager.registerEvents(EcoEnchantsHook(), SoulGraves.plugin)
		SoulGraves.plugin.logger.info("[√] EcoEnchants hook loaded!")
	}

	/**
	 * Makes sure soulbound items are not dropped on death
	 */
	@EventHandler
	fun onSoulSpawn(event: SoulSpawnEvent) {
		// Remove soulbound items from the soul's inventory
		event.soul.inventory.forEachIndexed { index, item ->
			if (item != null) {
				if (item.enchantments.filter { it.key.key.toString() == "minecraft:soulbound" }.isNotEmpty()) {
					event.soul.inventory[index] = null
				}
			}
		}

		// Don't need to restore soulbound items for EcoEnchants, as it handles them automatically.
	}
}