package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.utils.SoulState
import dev.faultyfunctions.soulgraves.api.event.SoulExplodeEvent
import dev.faultyfunctions.soulgraves.database.MessageAction
import dev.faultyfunctions.soulgraves.database.RedisDatabase
import dev.faultyfunctions.soulgraves.database.RedisPacket
import dev.faultyfunctions.soulgraves.managers.*
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class SoulExplodeTask(val soul: Soul) : BukkitRunnable() {

	override fun run() {
		if (soul.state == SoulState.EXPLODING) {
			soul.location.world?.loadChunk(soul.location.chunk)

			// CALL EVENT
			val soulSpawnEvent = SoulExplodeEvent(soul)
			Bukkit.getPluginManager().callEvent(soulSpawnEvent)
			if (soulSpawnEvent.isCancelled) { return }

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
			if (ConfigManager.burstSound.enabled) {
				ConfigManager.burstSound.sounds.forEachIndexed { index, soundKey ->
					soul.location.world?.playSound(soul.location, soundKey, ConfigManager.burstSound.volumes[index], ConfigManager.burstSound.pitches[index])
				}
			}

			if (ConfigManager.notifyOwnerBurstSound.enabled) {
				ConfigManager.notifyOwnerBurstSound.sounds.forEachIndexed { index, soundKey ->
					owner?.playSound(owner.location, soundKey, ConfigManager.notifyOwnerBurstSound.volumes[index], ConfigManager.notifyOwnerBurstSound.pitches[index])
				}
			}

			// SPAWN PARTICLES
			soul.location.world?.spawnParticle(Particle.POOF, soul.location.clone().add(0.0, 1.0, 0.0), 50, 0.0, 0.0, 0.0, 0.1, null, true)
			soul.location.world?.spawnParticle(Particle.END_ROD, soul.location.clone().add(0.0, 1.0, 0.0), 200, 0.0, 0.0, 0.0, 0.5, null, true)
			soul.location.world?.spawnParticle(Particle.SCULK_SOUL, soul.location.clone().add(0.0, 1.0, 0.0), 100, 0.0, 0.0, 0.0, 0.1, null, true)

			// SEND PLAYER MESSAGE
			when (DatabaseManager.storageMode) {
				STORAGE_TYPE.PDC -> {
					owner?.let {
						if (MessageManager.soulBurstComponent != null)
							SoulGraves.plugin.adventure().player(owner).sendMessage(MessageManager.soulBurstComponent!!)
						if (ConfigManager.soulsDropItems && MessageManager.soulBurstDropItemsComponent != null)
							SoulGraves.plugin.adventure().player(owner).sendMessage(MessageManager.soulBurstDropItemsComponent!!)
						else if (MessageManager.soulBurstLoseItemsComponent != null)
							SoulGraves.plugin.adventure().player(owner).sendMessage(MessageManager.soulBurstLoseItemsComponent!!)
					}
				}
				STORAGE_TYPE.DATABASE -> {
					RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.NOTIFY_SOUL_EXPLODE, soul.ownerUUID.toString()))
				}
			}

			// SEND NEARBY PLAYERS A MESSAGE
			if (ConfigManager.notifyNearbyPlayers) {
				val radii: Double = ConfigManager.notifyRadius.toDouble()
				soul.location.world?.getNearbyEntities(soul.location, radii, radii, radii)
					?.filterIsInstance<Player>()
					?.filterNot { it.uniqueId == soul.ownerUUID }
					?.forEach { player ->
						// Send Message
						MessageManager.soulBurstNearbyComponent?.let { SoulGraves.plugin.adventure().player(player).sendMessage(it) }
						// Play Sounds
						if (ConfigManager.notifyNearbySound.enabled) {
							ConfigManager.notifyNearbySound.sounds.forEachIndexed { index, soundKey ->
								player.playSound(player.location, soundKey, ConfigManager.notifyNearbySound.volumes[index], ConfigManager.notifyNearbySound.pitches[index])
							}
						}
					}
			}

			// REMOVE SOUL
			soul.delete()

			// UNLOAD CHUNK
			soul.location.world?.unloadChunk(soul.location.chunk)
		}
	}
}