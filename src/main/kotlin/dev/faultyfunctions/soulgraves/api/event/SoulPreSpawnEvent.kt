package dev.faultyfunctions.soulgraves.api.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent

class SoulPreSpawnEvent (var player: Player, var deathEvent: PlayerDeathEvent): Event(), Cancellable {

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

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

}