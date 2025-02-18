package dev.faultyfunctions.soulgraves.utils

import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import dev.faultyfunctions.soulgraves.api.RedisPublishAPI
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.DatabaseManager
import dev.faultyfunctions.soulgraves.managers.STORE_MODE
import dev.faultyfunctions.soulgraves.tasks.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Marker
import org.bukkit.inventory.ItemStack
import java.util.UUID

enum class SoulState {
	NORMAL, PANIC, EXPLODING
}

/**
 * Soul Create:
 * 1. Create Instance -> 2. SpawnMarker -> 3. StartTasks -> 4. SaveData
 */
class Soul(
	val ownerUUID: UUID,
	var markerUUID: UUID?,
	val location: Location,
	var inventory:MutableList<ItemStack?>,
	var xp: Int,
	var timeLeft: Int,
	val serverId: String = DatabaseManager.serverName,
	val expireTime: Long = System.currentTimeMillis() + ((ConfigManager.timeStable + ConfigManager.timeUnstable) * 1000)
) {
	var state: Enum<SoulState> = SoulState.NORMAL
	var implosion: Boolean = false

	private var explodeTask: SoulExplodeTask? = null
	private var particleTask: SoulParticleTask? = null
	private var pickupTask: SoulPickupTask? = null
	private var renderTask: SoulRenderTask? = null
	private var soundTask: SoulSoundTask? = null
	private var stateTask: SoulStateTask? = null

	/**
	 * Spawn a Marker Entity upon Soul Created. If Entity of MarkerUUID is Exist, Marker Will DO NOT Spawn.
	 */
	fun spawnMarker(): Entity? {
		if (serverId != DatabaseManager.serverName) return null
		if (location.world == null) return null
		markerUUID?.let { Bukkit.getEntity(it)?.let { return null } }

		location.world?.let {
			location.chunk.load()
			val marker = it.spawnEntity(location, EntityType.MARKER) as Marker
			marker.isPersistent = true
			marker.isSilent = true
			marker.isInvulnerable = true
			// STORE DATA
			marker.persistentDataContainer.set(soulKey, DataType.BOOLEAN, true)
			marker.persistentDataContainer.set(soulOwnerKey, DataType.UUID, ownerUUID)
			marker.persistentDataContainer.set(soulInvKey, DataType.ITEM_STACK_ARRAY, inventory.toTypedArray())
			marker.persistentDataContainer.set(soulXpKey, DataType.INTEGER, xp)
			marker.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, timeLeft)
			markerUUID = marker.uniqueId
			return marker
		}
		return null
	}


	/**
	 * Start Soul Tasks.
	 */
	fun startTasks() {
		if (serverId != DatabaseManager.serverName) return

		if (!isValid()) delete() // IF SOUL NOT VALID, DATA WILL REMOVE
		explodeTask ?: SoulExplodeTask(this).also {
			explodeTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 20)
		}

		particleTask ?: SoulParticleTask(this).also {
			particleTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 50)
		}

		pickupTask ?: SoulPickupTask(this).also {
			pickupTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 4)
		}

		renderTask ?: SoulRenderTask(this).also {
			renderTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 1)
		}

		soundTask ?: SoulSoundTask(this).also {
			soundTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 50)
		}

		stateTask ?: SoulStateTask(this).also {
			stateTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 20)
		}
	}


	/**
	 * Save to Database
	 */
	fun saveData(marker: Entity?) {
		if (serverId == DatabaseManager.serverName) {
			SoulGraves.soulList.add(this)
			when (DatabaseManager.storeMode) {
				// PDC
				STORE_MODE.PDC -> {
					marker?.let {
						val chunkList: MutableList<Long>? = marker.world.persistentDataContainer.get(soulChunksKey, DataType.asList(DataType.LONG))
						if (chunkList != null && !chunkList.contains(SpigotCompatUtils.getChunkKey(marker.location.chunk))) {
							chunkList.add(SpigotCompatUtils.getChunkKey(marker.location.chunk))
							marker.world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
						} }
				}
				// MYSQL + REDIS
				STORE_MODE.DATABASE -> {
					marker?.let {
						Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
							MySQLDatabase.instance.saveSoul(this, DatabaseManager.serverName)
						}) }
				}
			}
		}
	}


	/**
	 * Check Soul is Valid
	 */
	fun isValid(): Boolean {
		if (serverId != DatabaseManager.serverName) return false // not same server
		location.world ?: return false // world is not exist
		markerUUID ?: return false // initialization error
		markerUUID?.let { Bukkit.getEntity(it) ?: return false } // Marker is not exist
		return true
	}


	/**
	 * Make Soul Explode Now, Will Drop Exp And Items.
	 */
	fun explodeNow() {
		if (serverId == DatabaseManager.serverName) {
			this.state = SoulState.EXPLODING
			this.implosion = true
		} else {
			if (DatabaseManager.storeMode == STORE_MODE.DATABASE)
				markerUUID?.let { RedisPublishAPI.explodeSoul(it) }
		}
	}


	/**
	 * Delete Soul, Stop All Task of Soul, Will Drop Nothing.
	 */
	fun delete() {
		if (serverId == DatabaseManager.serverName) {
			explodeTask?.cancel()
			particleTask?.cancel()
			pickupTask?.cancel()
			renderTask?.cancel()
			soundTask?.cancel()
			stateTask?.cancel()

			SoulGraves.soulList.remove(this)
			when (DatabaseManager.storeMode) {
				// PDC - REMOVE CHUNK FROM LOAD LIST IF POSSIBLE
				STORE_MODE.PDC -> {
					var removeChunk = true
					for (entityInChunk in location.chunk.entities) {
						if (entityInChunk.persistentDataContainer.has(soulKey) && markerUUID != entityInChunk.uniqueId) {
							removeChunk = false
							break
						}
					}
					if (removeChunk) {
						val chunkList: MutableList<Long>? = location.world?.persistentDataContainer?.get(soulChunksKey, DataType.asList(DataType.LONG))
						if (chunkList != null) {
							chunkList.remove(SpigotCompatUtils.getChunkKey(location.chunk))
							location.world?.persistentDataContainer?.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
						}
					}
				}
				// MYSQL
				STORE_MODE.DATABASE -> {
					Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable { MySQLDatabase.instance.deleteSoul(this) })
				}
			}

			markerUUID?.let { uuid ->
				(Bukkit.getEntity(uuid) as? Marker)?.remove()
			}
		// IF NOT THIS SERVER, IT WILL PUB TO CHANNEL.
		} else {
			if (DatabaseManager.storeMode == STORE_MODE.DATABASE)
				markerUUID?.let { RedisPublishAPI.deleteSoul(it) }
		}
	}

}