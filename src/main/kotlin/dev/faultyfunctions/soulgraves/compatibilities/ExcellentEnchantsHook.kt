package dev.faultyfunctions.soulgraves.compatibilities

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.event.SoulSpawnEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class ExcellentEnchantsHook : Listener {
    companion object {
        val instance: ExcellentEnchantsHook by lazy { ExcellentEnchantsHook() }
    }

    fun registerEvents() {
        SoulGraves.plugin.server.pluginManager.registerEvents(ExcellentEnchantsHook(), SoulGraves.plugin)
        SoulGraves.plugin.logger.info("[√] ExcellentEnchants hook loaded!")
    }

    /**
     * Makes sure soulbound items are not dropped on death
     */
    @EventHandler
    fun onSoulSpawn(event: SoulSpawnEvent) {
        // Remove soulbound items from the soul's inventory
        event.soul.inventory.forEachIndexed { index, item ->
            if (item != null) {
                if (item.enchantments.filter { it.key.key.toString() == "excellentenchants:soulbound" }.isNotEmpty()) {
                    event.soul.inventory[index] = null
                }
            }
        }

        // Don't need to restore soulbound items for EcoEnchants, as it handles them automatically.
    }
}