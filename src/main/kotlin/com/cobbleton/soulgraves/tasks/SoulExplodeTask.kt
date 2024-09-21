package com.cobbleton.soulgraves.tasks

import com.cobbleton.soulgraves.SoulGraves
import com.cobbleton.soulgraves.soulChunksKey
import com.cobbleton.soulgraves.soulKey
import com.cobbleton.soulgraves.utils.SoulState
import com.jeff_media.morepersistentdatatypes.DataType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class SoulExplodeTask : BukkitRunnable() {
	override fun run() {
		val soulIterator = SoulGraves.soulList.iterator()
		while (soulIterator.hasNext()) {
			val soul = soulIterator.next()
			if (soul.state == SoulState.EXPLODING) {
				soul.location.world.loadChunk(soul.location.chunk)

				soul.inventory.forEach {i ->
					if (i != null) {
						soul.location.world.dropItemNaturally(soul.location, i)
					}
				}
				if (soul.xp != 0) {
					soul.location.world.spawn(soul.location, ExperienceOrb::class.java).experience = (soul.xp * 0.2).toInt()
				}

				val owner: Player? = Bukkit.getPlayer(soul.ownerUUID)

				// PLAY SOUNDS
				soul.location.world.playSound(soul.location, Sound.BLOCK_GLASS_BREAK, 3.0f, 1.0f)
				soul.location.world.playSound(soul.location, Sound.ENTITY_VEX_DEATH, 3.0f, 0.5f)
				soul.location.world.playSound(soul.location, Sound.ENTITY_ALLAY_DEATH, 3.0f, 0.5f)
				soul.location.world.playSound(soul.location, Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 0.5f)
				owner?.playSound(owner.location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.5f)

				// SPAWN PARTICLES
				soul.location.world.spawnParticle(Particle.POOF, soul.location.clone().add(0.0, 1.0, 0.0), 50, 0.0, 0.0, 0.0, 0.1, null, true)
				soul.location.world.spawnParticle(Particle.END_ROD, soul.location.clone().add(0.0, 1.0, 0.0), 200, 0.0, 0.0, 0.0, 0.5, null, true)
				soul.location.world.spawnParticle(Particle.SCULK_SOUL, soul.location.clone().add(0.0, 1.0, 0.0), 100, 0.0, 0.0, 0.0, 0.1, null, true)

				// SEND PLAYER MESSAGE
				owner?.sendMessage(Component.text(""))
				owner?.sendMessage(Component.text("☠ Your soul is no longer stable!", NamedTextColor.DARK_AQUA))
				owner?.sendMessage(Component.text("☀ Any belongings inside have been scattered!", NamedTextColor.RED))
				owner?.sendMessage(Component.text(""))

				// REMOVE CHUNK FOR LOAD LIST IF POSSIBLE
				var removeChunk = true
				for (entity in soul.location.chunk.entities) {
					if (!entity.isDead && entity.persistentDataContainer.has(soulKey) && soul.entityUUID != entity.uniqueId) {
						removeChunk = false
						break
					}
				}
				if (removeChunk) {
					val chunkList: MutableList<Long>? = soul.location.world.persistentDataContainer.get(soulChunksKey, DataType.asList(DataType.LONG))
					if (chunkList != null) {
						chunkList.remove(soul.location.chunk.chunkKey)
						soul.location.world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
					}
				}
				// REMOVE SOUL
				val soulEntity: Marker = soul.location.world.getEntity(soul.entityUUID) as Marker
				soulEntity.remove()
				soulIterator.remove()

				// UNLOAD CHUNK
				soul.location.world.unloadChunk(soul.location.chunk)
			}
		}
	}
}