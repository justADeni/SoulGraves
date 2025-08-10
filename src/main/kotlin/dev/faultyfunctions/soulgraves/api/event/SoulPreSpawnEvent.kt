package dev.faultyfunctions.soulgraves.api.event

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent

class SoulPreSpawnEvent (var deathEvent: PlayerDeathEvent): Event(), Cancellable {
    private var isCancelled = false
    val isNotCancelled: Boolean
		get() = !isCancelled
    var keepInventory: Boolean = false
    var keepLevel: Boolean = false

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

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}