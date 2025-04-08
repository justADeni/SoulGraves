package dev.faultyfunctions.soulgraves.api.event

import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack

class SoulSpawnEvent (var player: Player, var soul: Soul): Event() {

    companion object {
        val HANDLER_LIST = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLER_LIST
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }

    fun getStorageItems(): MutableList<ItemStack?> {
        return soul.inventory
    }

    fun getSoulLocation(): Location {
        return soul.location
    }


}