package dev.faultyfunctions.soulgraves.api

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.database.MessageAction
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.database.RedisDatabase
import dev.faultyfunctions.soulgraves.database.RedisPacket
import dev.faultyfunctions.soulgraves.managers.SERVER_NAME
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.STORAGE_TYPE
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


/**
 * In this API, ALL Action Will Cross Server.
 * If Target Server Offline, It Will Mark To Mysql, Target Server Will Handle These Souls At Next Start.
 */
object RedisPublishAPI {

    val pendingAnswersRequests = ConcurrentHashMap<String, CompletableFuture<Boolean>>()

    /**
     * Remove a Soul Cross server
     * 1. Function will check Soul exists in the database first. If not exist, no operation will be performed.
     *
     * @return whether the auction is successful
     */
    fun deleteSoul(markerUUID: UUID): CompletableFuture<Boolean> {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE REDIS PUBLISH API WITH PDC STORAGE MODE!")

        val future = CompletableFuture<Boolean>()
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            try {
                val soul = MySQLDatabase.instance.getSoul(markerUUID)
                if (soul == null) {
                    future.complete(false)
                    return@Runnable
                }
                if (RedisDatabase.instance.isServerOnline(soul.serverId)) {
                    val msgUUID = UUID.randomUUID()
                    val target = soul.serverId
                    pendingAnswersRequests[msgUUID.toString()] = future
                    future.whenComplete { _, _ ->
                        pendingAnswersRequests.remove(msgUUID.toString())
                    }
                    RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.REMOVE_SOUL, "$msgUUID|$target|$markerUUID"))
                // IF Target Server is Offline.
                } else {
                    MySQLDatabase.instance.markSoulDelete(markerUUID)
                    future.complete(true)
                }
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        })
        return future.orTimeout(5, TimeUnit.SECONDS)
    }


    /**
     * Make a Soul Explode server
     * 1. Function will check Soul exists in the database first. If not exist, no operation will be performed.
     *
     * @return whether the auction is successful
     */
    fun explodeSoul(markerUUID: UUID): CompletableFuture<Boolean> {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE REDIS PUBLISH API WITH PDC STORAGE MODE!")

        val future = CompletableFuture<Boolean>()
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            try {
                val soul = MySQLDatabase.instance.getSoul(markerUUID)
                if (soul == null) {
                    future.complete(false)
                    return@Runnable
                }
                if (RedisDatabase.instance.isServerOnline(soul.serverId)) {
                    val msgUUID = UUID.randomUUID()
                    val target = soul.serverId
                    pendingAnswersRequests[msgUUID.toString()] = future
                    future.whenComplete { _, _ ->
                        pendingAnswersRequests.remove(msgUUID.toString())
                    }
                    RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.EXPLODE_SOUL, "$msgUUID|$target|$markerUUID"))

                // IF Target Server is Offline.
                } else {
                    MySQLDatabase.instance.markSoulExplode(markerUUID)
                    future.complete(true)
                }
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        })
        return future.orTimeout(5, TimeUnit.SECONDS)
    }


    /**
     * Experimental
     * Send a Message to Update Modified Soul data copy form Database.
     * Can Sync Field: ownerUUID, inventory, xp, expireTime, freezeTime(0 is not freeze)
     * 1. Function will check Soul exists in the database first. If not exist, no operation will be performed.
     * 2. Use this feature carefully to avoid multiple concurrent situations where their data maybe randomly overwrite each other due to network delays.
     * 3. Pay attention to ExpireTime/FreezeTime, perhaps the target soul extended the time/froze the soul on the Origin server.
     *
     * @return whether the auction is successful
     */
    fun syncSoul(soul: Soul): CompletableFuture<Boolean> {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE REDIS PUBLISH API WITH PDC STORAGE MODE!")

        val future = CompletableFuture<Boolean>()
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            try {
                val checkSoul = MySQLDatabase.instance.getSoul(soul.markerUUID)
                if (checkSoul == null) {
                    future.complete(false)
                    return@Runnable
                }
                MySQLDatabase.instance.saveSoulCopy(soul) // Save Copy

                val msgUUID = UUID.randomUUID()
                pendingAnswersRequests[msgUUID.toString()] = future
                future.whenComplete { _, _ ->
                    pendingAnswersRequests.remove(msgUUID.toString())
                }
                RedisDatabase.instance.publish(RedisPacket(senderId = SERVER_NAME, action = MessageAction.UPDATE_SOUL, payload = soul.markerUUID.toString()))
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        })

        return future.orTimeout(5, TimeUnit.SECONDS)
    }

}