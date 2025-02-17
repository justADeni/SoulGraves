package dev.faultyfunctions.soulgraves.api

import dev.faultyfunctions.soulgraves.SoulGraves.Companion.soulList
import dev.faultyfunctions.soulgraves.utils.Soul
import dev.faultyfunctions.soulgraves.utils.SoulState
import java.util.*

object SoulGraveAPI {

    /**
     * Returns a list of all souls in current server.
     */
    fun getAllSouls(): MutableList<Soul> {
        return soulList
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
     * Makes a soul instantly explode.
     */
    fun makeExplosion(soul: Soul) {
        soul.explodeNow()
    }

}