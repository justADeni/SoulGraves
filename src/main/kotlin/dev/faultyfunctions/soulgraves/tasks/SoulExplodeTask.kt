package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import dev.faultyfunctions.soulgraves.soulChunksKey
import dev.faultyfunctions.soulgraves.soulKey
import dev.faultyfunctions.soulgraves.utils.SoulState
import com.jeff_media.morepersistentdatatypes.DataType
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
				soul.location.world?.loadChunk(soul.location.chunk)

				// DROP ITEMS
				if (ConfigManager.soulsDropItems) {
					soul.inventory.forEach { i ->
						if (i != null) {
							soul.location.world?.dropItemNaturally(soul.location, i)
						}
					}
				}

				// DROP XP
				if (ConfigManager.soulsDropXP && soul.xp != 0) {
					soul.location.world?.spawn(soul.location, ExperienceOrb::class.java)?.experience = (soul.xp * ConfigManager.xpPercentageBurst).toInt()
				}

				val owner: Player? = Bukkit.getPlayer(soul.ownerUUID)

				// PLAY SOUNDS
				soul.location.world?.playSound(soul.location, Sound.BLOCK_GLASS_BREAK, 3.0f, 1.0f)
				soul.location.world?.playSound(soul.location, Sound.ENTITY_VEX_DEATH, 3.0f, 0.5f)
				soul.location.world?.playSound(soul.location, Sound.ENTITY_ALLAY_DEATH, 3.0f, 0.5f)
				soul.location.world?.playSound(soul.location, Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 0.5f)
				owner?.playSound(owner.location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.5f)

				// SPAWN PARTICLES
				soul.location.world?.spawnParticle(Particle.POOF, soul.location.clone().add(0.0, 1.0, 0.0), 50, 0.0, 0.0, 0.0, 0.1, null, true)
				soul.location.world?.spawnParticle(Particle.END_ROD, soul.location.clone().add(0.0, 1.0, 0.0), 200, 0.0, 0.0, 0.0, 0.5, null, true)
				soul.location.world?.spawnParticle(Particle.SCULK_SOUL, soul.location.clone().add(0.0, 1.0, 0.0), 100, 0.0, 0.0, 0.0, 0.1, null, true)

				// SEND PLAYER MESSAGE
				if (owner != null) {
					SoulGraves.plugin.adventure().player(owner).sendMessage(MessageManager.soulBurstComponent)
					SoulGraves.plugin.adventure().player(owner).sendMessage(if (ConfigManager.soulsDropItems) { MessageManager.soulBurstDropItemsComponent } else { MessageManager.soulBurstLoseItemsComponent })
				}

				// SEND NEARBY PLAYERS A MESSAGE
				val marker: Marker = Bukkit.getEntity(soul.markerUUID) as Marker
				if (ConfigManager.notifyNearbyPlayers) {
					val radii: Double = ConfigManager.notifyRadius.toDouble()
					for (player in marker.getNearbyEntities(radii, radii, radii)) {
						if (player.uniqueId != soul.ownerUUID) {
							SoulGraves.plugin.adventure().player(player.uniqueId).sendMessage(MessageManager.soulBurstNearbyComponent)
							Bukkit.getPlayer(player.uniqueId)?.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f)
						}
					}
				}

				// REMOVE CHUNK FOR LOAD LIST IF POSSIBLE
				var removeChunk = true
				for (entity in soul.location.chunk.entities) {
					if (!entity.isDead && entity.persistentDataContainer.has(soulKey) && soul.markerUUID != entity.uniqueId) {
						removeChunk = false
						break
					}
				}
				if (removeChunk) {
					val chunkList: MutableList<Long>? = soul.location.world?.persistentDataContainer?.get(soulChunksKey, DataType.asList(DataType.LONG))
					if (chunkList != null) {
						chunkList.remove(SoulGraves.compat.getChunkKey(soul.location.chunk))
						soul.location.world?.persistentDataContainer?.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
					}
				}

				// REMOVE SOUL
				marker.remove()
				soulIterator.remove()

				// UNLOAD CHUNK
				soul.location.world?.unloadChunk(soul.location.chunk)
			}
		}
	}
}