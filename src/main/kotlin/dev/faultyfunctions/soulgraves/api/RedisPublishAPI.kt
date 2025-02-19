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


/**
 * In this API, ALL Action Will Cross Server.
 * If Target Server Offline, It Will Mark To Mysql, Target Server Will Handle These Souls At Next Start.
 */
object RedisPublishAPI {

    /**
     * Remove a Soul Cross server
     */
    fun deleteSoul(markerUUID: UUID) {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE REDIS PUBLISH API WITH PDC STORAGE MODE!")
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            MySQLDatabase.instance.markSoulDelete(markerUUID)
            RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.REMOVE_SOUL, markerUUID.toString()))
        })
    }


    /**
     * Make a Soul Explode server
     */
    fun explodeSoul(markerUUID: UUID) {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE REDIS PUBLISH API WITH PDC STORAGE MODE!")
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            MySQLDatabase.instance.markSoulExplode(markerUUID)
            RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.EXPLODE_SOUL, markerUUID.toString()))
        })
    }


    /**
     * Send a Message to Update Modified Soul data copy form Database.
     */
    fun syncSoul(soul: Soul) {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE REDIS PUBLISH API WITH PDC STORAGE MODE!")
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            MySQLDatabase.instance.saveSoul(soul)
            RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.UPDATE_SOUL, soul.markerUUID.toString()))
        })
    }

}