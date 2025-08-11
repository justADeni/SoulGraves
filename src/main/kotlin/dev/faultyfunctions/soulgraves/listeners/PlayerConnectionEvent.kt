package dev.faultyfunctions.soulgraves.listeners

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.SoulGravesAPI
import dev.faultyfunctions.soulgraves.compatibilities.VaultHook
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.StorageType
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.math.BigDecimal

class PlayerConnectionEvent : Listener {
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (!ConfigManager.offlineOwnerTimerFreeze) return
        val now = System.currentTimeMillis()
        val playerSouls = SoulGravesAPI.getPlayerSouls(e.player.uniqueId)
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
    fun onPlayerTeleportToSoulCrossServer(e: PlayerJoinEvent) {
        SoulGraves.pendingTeleports.remove(e.player.uniqueId)?.let { location ->
            Bukkit.getScheduler().runTaskLater(SoulGraves.plugin, Runnable {
                val player = Bukkit.getPlayer(e.player.uniqueId) ?: return@Runnable
                if (player.isOnline) {
                    // IF THERE IS NO TELEPORT COST DEFINED IN THE CONFIG
                    if (ConfigManager.teleportCost <= BigDecimal.ZERO) {
                        MessageManager.commandBackSuccessFree?.let {
                            SoulGraves.plugin.adventure().player(player).sendMessage(it)
                        }
                        player.teleport(location)
                    } else {
                        // IF PLAYER HAS ENOUGH FUNDS
                        if (VaultHook.has(player, ConfigManager.teleportCost)) {
                            // REMOVE FUNDS, SEND MESSAGE, AND TELEPORT
                            VaultHook.withdraw(player, ConfigManager.teleportCost)
                            MessageManager.commandBackSuccessPaid?.let {
                                SoulGraves.plugin.adventure().player(player).sendMessage(it)
                            }
                            player.teleport(location)
                        } else {
                            // SEND NO FUNDS MESSAGE
                            MessageManager.commandBackNoFundsComponent?.let {
                                SoulGraves.plugin.adventure().player(player).sendMessage(it)
                            }
                        }
                    }
                }
            }, 100L)
        }
    }

    @EventHandler
    fun onPlayerLeave(e: PlayerQuitEvent) {
        if (!ConfigManager.offlineOwnerTimerFreeze) return
        val now = System.currentTimeMillis()
        val playerSouls = SoulGravesAPI.getPlayerSouls(e.player.uniqueId)
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