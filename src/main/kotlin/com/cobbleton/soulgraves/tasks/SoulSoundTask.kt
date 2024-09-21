package com.cobbleton.soulgraves.tasks

import com.cobbleton.soulgraves.SoulGraves
import com.cobbleton.soulgraves.utils.SoulState
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable

class SoulSoundTask : BukkitRunnable() {
	override fun run() {
		val soulIterator = SoulGraves.soulList.iterator()
		while (soulIterator.hasNext()) {
			val soul = soulIterator.next()
			if (!soul.location.isChunkLoaded) { continue }

			if (soul.state == SoulState.NORMAL) {
				soul.location.world.playSound(soul.location, Sound.ENTITY_WARDEN_HEARTBEAT, 3.0f, 1.0f)
			}

			if (soul.state == SoulState.PANIC) {
				soul.location.world.playSound(soul.location, Sound.ENTITY_WARDEN_HEARTBEAT, 3.0f, 0.5f)
				soul.location.world.playSound(soul.location, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 8.0f, 0.5f)
				soul.location.world.spawnParticle(Particle.END_ROD, soul.location.clone().add(0.0, 1.0, 0.0), 75, 0.0, 0.0, 0.0, 0.1, null, true)
			}
		}
	}
}