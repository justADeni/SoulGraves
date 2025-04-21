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
        // STORE SOULBOUND ITEMS
        val soulboundInventory: MutableList<ItemStack?> = mutableListOf()
        event.soul.inventory.forEachIndexed { index, item ->
            if (item != null) {
                if (item.enchantments.filter { it.key.key.toString() == "minecraft:soulbound" }.isNotEmpty()) {
                    event.soul.inventory[index] = null
                    soulboundInventory.add(item)
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
                        event.player.inventory.setItem(index, item)
                    }
                }
            }, 1L)
        }
    }
}