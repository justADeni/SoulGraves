package dev.faultyfunctions.soulgraves.api.event

import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack

class SoulExplodeEvent (var soul: Soul): Event(), Cancellable {

    private var isCancelled = false

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

    fun setStorageItems(storageItems: MutableList<ItemStack?>) {
        soul.inventory = storageItems
    }

    fun getOwner(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(soul.ownerUUID)
    }

    fun getSoulLocation(): Location {
        return soul.location
    }

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

}