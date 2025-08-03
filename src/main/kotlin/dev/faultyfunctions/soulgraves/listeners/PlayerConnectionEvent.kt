package dev.faultyfunctions.soulgraves.listeners

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.SoulGraveAPI
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.StorageType
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerConnectionEvent : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!ConfigManager.offlineOwnerTimerFreeze) return
        val now = System.currentTimeMillis()
        val playerSouls = SoulGraveAPI.getPlayerSouls(event.player.uniqueId)
        playerSouls.forEach {
            val offlineTime = now - it.freezeTime
            it.expireTime += offlineTime
            it.freezeTime = 0L
            if (STORAGE_MODE == StorageType.CROSS_SERVER) {
                Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
                    MySQLDatabase.instance.updateSoulFreezeTime(it.markerUUID, 0L, it.expireTime)
                })
            }
        }
    }


    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        if (!ConfigManager.offlineOwnerTimerFreeze) return
        val now = System.currentTimeMillis()
        val playerSouls = SoulGraveAPI.getPlayerSouls(event.player.uniqueId)
        playerSouls.forEach {
            it.freezeTime = now
            if (STORAGE_MODE == StorageType.CROSS_SERVER) {
                Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
                    MySQLDatabase.instance.updateSoulFreezeTime(it.markerUUID, now, it.expireTime)
                })
            }
        }
    }
}