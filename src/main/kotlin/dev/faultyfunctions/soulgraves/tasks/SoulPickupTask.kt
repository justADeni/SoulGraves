package dev.faultyfunctions.soulgraves.tasks

import org.bukkit.scheduler.BukkitRunnable
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
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SoulPickupTask : BukkitRunnable() {
	override fun run() {
		val soulIterator = SoulGraves.soulList.iterator()
		while (soulIterator.hasNext()) {
			val soul = soulIterator.next()

			if (!soul.location.world?.isChunkLoaded(soul.location.chunk)!! || soul.state == SoulState.EXPLODING) { continue }

			val marker: Marker = Bukkit.getEntity(soul.markerUUID) as Marker

			for (entity in marker.getNearbyEntities(0.5, 0.5, 0.5)) {
				if (entity !is Player) { continue }

				val player: Player = entity
				if (!player.isDead) {
					// CHECK IF PLAYER NEEDS TO BE OWNER
					if (ConfigManager.ownerLocked && (player.uniqueId != soul.ownerUUID)) { continue }

					// HANDLE INVENTORY
					val missedItems: MutableList<ItemStack> = mutableListOf()
					soul.inventory.forEachIndexed { index, item ->
						if (item != null) {
							if (player.inventory.getItem(index) == null) {
								player.inventory.setItem(index, item)
							} else {
								missedItems.add(item)
							}
						}
					}
					val missHashMap = player.inventory.addItem(*missedItems.toTypedArray())
					missHashMap.forEach { (_, item) ->
						soul.location.world!!.dropItem(soul.location, item)
					}

					// HANDLE XP
					val owner: Player? = Bukkit.getPlayer(soul.ownerUUID)
					val xpMultiplier = if (player.uniqueId == owner?.uniqueId) ConfigManager.xpPercentageOwner else ConfigManager.xpPercentageOthers
					player.giveExp((soul.xp * xpMultiplier).toInt())

					// PLAY SOUNDS
					player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.5f)
					player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f)
					player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f)

					// SPAWN PARTICLES
					player.world.spawnParticle(Particle.END_ROD, soul.location, 200, 1.0, 1.0, 1.0, 0.5)
					player.world.spawnParticle(Particle.FIREWORK, soul.location, 50, 1.0, 1.0, 1.0, 0.1)

					// SEND MESSAGE TO PLAYER
					SoulGraves.plugin.adventure().player(player).sendMessage(MessageManager.soulCollectComponent)

					// SEND MESSAGE TO OWNER IF NEEDED
					if (player.uniqueId != soul.ownerUUID && owner != null) {
						SoulGraves.plugin.adventure().player(owner.uniqueId).sendMessage(MessageManager.soulCollectOtherComponent)
						owner.playSound(owner.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f)
					}

					// REMOVE CHUNK FROM LOAD LIST IF POSSIBLE
					var removeChunk = true
					for (entityInChunk in soul.location.chunk.entities) {
						if (entityInChunk.persistentDataContainer.has(soulKey) && soul.markerUUID != entityInChunk.uniqueId) {
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
				}
			}
		}
	}
}