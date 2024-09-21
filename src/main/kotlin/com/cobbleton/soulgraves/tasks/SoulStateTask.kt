package com.cobbleton.soulgraves.tasks

import com.cobbleton.soulgraves.SoulGraves
import com.cobbleton.soulgraves.soulTimeLeftKey
import com.cobbleton.soulgraves.utils.SoulState
import com.jeff_media.morepersistentdatatypes.DataType
import org.bukkit.entity.Marker
import org.bukkit.scheduler.BukkitRunnable

class SoulStateTask : BukkitRunnable() {
	override fun run() {
		for (soul in SoulGraves.soulList) {
			when (soul.timeLeft) {
				in 60..Int.MAX_VALUE -> soul.state = SoulState.NORMAL
				in 1..59 -> soul.state = SoulState.PANIC
				else -> soul.state = SoulState.EXPLODING
			}
			soul.timeLeft -= 1

			// LOAD CHUNK & GRAB ENTITY
			soul.location.world.loadChunk(soul.location.chunk)
			val soulEntity: Marker = soul.location.world.getEntity(soul.entityUUID) as Marker

			// STORE TIME LEFT IN ENTITY'S PDC
			soulEntity.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, soul.timeLeft)

			// UNLOAD CHUNK
			soul.location.world.unloadChunk(soul.location.chunk)
		}
	}
}