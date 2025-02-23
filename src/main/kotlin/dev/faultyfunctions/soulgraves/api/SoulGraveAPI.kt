package dev.faultyfunctions.soulgraves.api

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.SoulGraves.Companion.soulList
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.STORAGE_TYPE
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object SoulGraveAPI {

    /**
     * Returns a list of all souls in current server.
     */
    fun getAllSouls(): MutableList<Soul> {
        return soulList
    }

    /**
     * Returns a list of all souls cross server.
     */
    fun getAllSoulsCrossServer(): CompletableFuture<List<Soul>> {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE CROSS-SERVER API WITH PDC STORAGE MODE!")
        val future = CompletableFuture<List<Soul>>()
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            val allSouls = MySQLDatabase.instance.getAllSouls()
            future.complete(allSouls)
        })
        return future.orTimeout(5, TimeUnit.SECONDS)
    }


    /**
     * Returns a list of all souls in current server that match the given owner UUID.
     */
    fun getPlayerSouls(ownerUUID: UUID): MutableList<Soul> {
        val souls = mutableListOf<Soul>()
        for (soul in soulList) {
            if (soul.ownerUUID == ownerUUID) {
                souls.add(soul)
            }
        }
        return souls
    }

    /**
     * Returns a list of all souls cross server that match the given owner UUID.
     */
    fun getPlayerSoulsCrossServer(ownerUUID: UUID): CompletableFuture<List<Soul>> {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE CROSS-SERVER API WITH PDC STORAGE MODE!")
        val future = CompletableFuture<List<Soul>>()
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            val allSouls = MySQLDatabase.instance.getPlayerSouls(ownerUUID)
            future.complete(allSouls)
        })
        return future.orTimeout(5, TimeUnit.SECONDS)
    }


    /**
     * Returns a soul that matches the given marker UUID.
     */
    fun getSoul(makerUUID: UUID): Soul? {
        for (soul in soulList) {
            if (soul.markerUUID == makerUUID) {
                return soul
            }
        }
        return null
    }

    /**
     * Returns a souls cross server that match the given marker UUID.
     */
    fun getSoulCrossServer(makerUUID: UUID): CompletableFuture<Soul?> {
        if (STORAGE_MODE == STORAGE_TYPE.PDC) throw RuntimeException("DO NOT USE CROSS-SERVER API WITH PDC STORAGE MODE!")
        val future = CompletableFuture<Soul?>()
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            val soul = MySQLDatabase.instance.getSoul(makerUUID)
            future.complete(soul)
        })
        return future.orTimeout(5, TimeUnit.SECONDS)
    }


    /**
     * Makes a soul instantly explode.
     */
    fun makeExplosion(soul: Soul) {
        soul.explode()
    }

}