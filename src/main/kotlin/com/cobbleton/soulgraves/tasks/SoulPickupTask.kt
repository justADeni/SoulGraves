package com.cobbleton.soulgraves.tasks

import org.bukkit.scheduler.BukkitRunnable
import com.cobbleton.soulgraves.SoulGraves
import com.cobbleton.soulgraves.managers.ConfigManager
import com.cobbleton.soulgraves.managers.MessageManager
import com.cobbleton.soulgraves.soulChunksKey
import com.cobbleton.soulgraves.soulKey
import com.cobbleton.soulgraves.utils.SoulState
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

			if (!soul.location.isChunkLoaded || soul.state == SoulState.EXPLODING) { continue }

			for (player in soul.location.getNearbyPlayers(0.5)) {
				if (!player.isDead) {
					// CHECK IF PLAYER NEEDS TO BE OWNER
					if (ConfigManager.ownerLocked && (player.uniqueId != soul.ownerUUID)) { continue }

					val soulEntity: Marker = soul.location.world.getEntity(soul.entityUUID) as Marker

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
						soul.location.world.dropItem(soul.location, item)
					}

					// HANDLE XP
					val owner: Player? = Bukkit.getPlayer(soul.ownerUUID) // Not needed?
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
					player.sendMessage(MessageManager.soulCollectComponent)

					// SEND MESSAGE TO OWNER IF NEEDED
					if (player.uniqueId != soul.ownerUUID) {
						owner?.sendMessage(MessageManager.soulCollectOtherComponent)
						owner?.playSound(owner.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f)
					}

					// REMOVE CHUNK FOR LOAD LIST IF POSSIBLE
					var removeChunk = true
					for (entity in soul.location.chunk.entities) {
						if (entity.persistentDataContainer.has(soulKey) && soul.entityUUID != entity.uniqueId) {
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
					soulEntity.remove()
					soulIterator.remove()
				}
			}
		}
	}
}