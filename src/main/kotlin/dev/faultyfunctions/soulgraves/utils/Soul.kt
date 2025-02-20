package dev.faultyfunctions.soulgraves.utils

import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import dev.faultyfunctions.soulgraves.api.RedisPublishAPI
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.database.soulChunksKey
import dev.faultyfunctions.soulgraves.database.soulKey
import dev.faultyfunctions.soulgraves.managers.*
import dev.faultyfunctions.soulgraves.tasks.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Marker
import org.bukkit.inventory.ItemStack
import java.util.UUID

enum class SoulState {
	NORMAL, PANIC, EXPLODING
}

/**
 * Soul Create:
 * NewDeath -> 1. SpawnMarker -> 2. Create Instance -> 3. StartTasks -> 4. SaveData
 * DataCopy -> 1. ReadFromMysql -> 2. Create Instance
 * InitOnStart -> 1. ReadFromMysql/PDC -> 2. Create Instance -> 4. StartTasks
 */
class Soul private constructor(
	var ownerUUID: UUID,
	var markerUUID: UUID,
	var location: Location,
	var inventory:MutableList<ItemStack?>,
	var xp: Int,

	val deathTime: Long,
	expireTime: Long,
	timeLeft: Int, // Second

	val serverId: String = SERVER_NAME,
	val isLocal: Boolean = serverId == SERVER_NAME
) {

	var expireTime: Long = expireTime
		set(value) {
			field = value
			timeLeft = ((value - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
		}

	var timeLeft: Int = timeLeft
		set(value) {
			field = value.coerceAtLeast(0)
			expireTime = System.currentTimeMillis() + field * 1000L
		}
		get() = if (isLocal) field else ((expireTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)


	companion object {

		// Create Soul upon Player Dead.
		fun createNewForPlayerDeath(
			ownerUUID: UUID,
			marker: Entity,
			location: Location,
			inventory: MutableList<ItemStack?>,
			xp: Int,
			deathTime: Long,
			expireTime: Long
		): Soul {
			val soul = Soul(
				ownerUUID = ownerUUID,
				markerUUID = marker.uniqueId,
				location = location,
				inventory = inventory,
				xp = xp,

				deathTime = deathTime,
				expireTime = deathTime + ((ConfigManager.timeStable + ConfigManager.timeUnstable) * 1000),
				timeLeft = ((expireTime - deathTime) / 1000).toInt(),

				isLocal = true
			)
			// Init Soul
			soul.marker = marker
			soul.startTasks()
			soul.saveData()
			return soul
		}

		// Read from database by api.
		fun createDataCopy(
			ownerUUID: UUID,
			markerUUID: UUID,
			location: Location,
			inventory: MutableList<ItemStack?>,
			xp: Int,
			serverId: String,
			deathTime: Long,
			expireTime: Long
		) = Soul(
			markerUUID = markerUUID,
			ownerUUID = ownerUUID,
			location = location,
			inventory = inventory,
			xp = xp,

			deathTime = deathTime,
			expireTime = expireTime,
			timeLeft = ((expireTime - deathTime) / 1000).toInt(),

			serverId = serverId,
			isLocal = false
		)

		// Init from database or PDC
		fun initAndStart(
			markerUUID: UUID,
			ownerUUID: UUID,
			location: Location,
			inventory: MutableList<ItemStack?>,
			xp: Int,
			deathTime: Long,
			expireTime: Long,
		): Soul {
			val soul = Soul(
				markerUUID = markerUUID,
				ownerUUID = ownerUUID,
				location = location,
				inventory = inventory,
				xp = xp,

				deathTime = deathTime,
				expireTime = expireTime,
				timeLeft = ((expireTime - deathTime) / 1000).toInt(),

				isLocal = true
			)
			// Init Soul
			SoulGraves.soulList.add(soul)
			soul.startTasks()
			return soul
		}

	}


	var marker: Entity? = null
	var state: Enum<SoulState> = SoulState.NORMAL
	var implosion: Boolean = false

	private var explodeTask: SoulExplodeTask? = null
	private var particleTask: SoulParticleTask? = null
	private var pickupTask: SoulPickupTask? = null
	private var renderTask: SoulRenderTask? = null
	private var soundTask: SoulSoundTask? = null
	private var stateTask: SoulStateTask? = null
	private var validationTask: SoulValidationTask? = null

	/**
	 * Start Soul Tasks.
	 */
	fun startTasks() {
		// If not local soul
		if (!isLocal) return

		// If world is not exist, entity is not exist
		location.world?.let { world ->
			world.loadChunk(location.chunk)
			Bukkit.getEntity(markerUUID) ?: delete()
		} ?: delete()

		// Start Tasks
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

		validationTask ?: SoulValidationTask(this).also {
			validationTask = it
			it.runTaskTimer(SoulGraves.plugin, 0, 100)
		}
	}


	/**
	 * Save to Database on Created
	 */
	private fun saveData() {
		when (STORAGE_MODE) {
			// PDC
			STORAGE_TYPE.PDC -> {
				marker?.let {
					val chunkList: MutableList<Long>? = it.world.persistentDataContainer.get(soulChunksKey, DataType.asList(DataType.LONG))
					if (chunkList != null && !chunkList.contains(SpigotCompatUtils.getChunkKey(it.location.chunk))) {
						chunkList.add(SpigotCompatUtils.getChunkKey(it.location.chunk))
						it.world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), chunkList)
					}
				}
			}
			// DATABASE
			STORAGE_TYPE.DATABASE -> {
				marker?.let {
					Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
						MySQLDatabase.instance.saveSoul(this)
					})
				}
			}
		}
	}


	/**
	 * Make Soul Explode Now, Will Drop Exp And Items.
	 */
	fun explode() {
		when {
			// Local
			isLocal -> {
				this.state = SoulState.EXPLODING
				this.implosion = true
			}

			// REMOTE
			STORAGE_MODE == STORAGE_TYPE.DATABASE -> {
				RedisPublishAPI.explodeSoul(markerUUID)
			}
		}
	}


	/**
	 * Delete Soul, Stop All Task of Soul, Will Drop Nothing.
	 */
	fun delete() {
		when {
			// PDC
			isLocal && STORAGE_MODE == STORAGE_TYPE.PDC -> {
				// CANCEL TASKS
				explodeTask?.cancel()
				particleTask?.cancel()
				pickupTask?.cancel()
				renderTask?.cancel()
				soundTask?.cancel()
				stateTask?.cancel()
				validationTask?.cancel()
				// REMOVE DATA FROM PDC
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
				// REMOVE ENTITY
				location.world?.loadChunk(location.chunk).apply {
					(Bukkit.getEntity(markerUUID) as? Marker)?.remove()
				}
			}

			// DATABASE
			isLocal && STORAGE_MODE == STORAGE_TYPE.DATABASE -> {
				// CANCEL TASKS
				explodeTask?.cancel()
				particleTask?.cancel()
				pickupTask?.cancel()
				renderTask?.cancel()
				soundTask?.cancel()
				stateTask?.cancel()
				validationTask?.cancel()
				// REMOVE DATA FROM DATABASE
				Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
					MySQLDatabase.instance.deleteSoul(this)
				})
				// REMOVE ENTITY
				location.world?.loadChunk(location.chunk)
				(Bukkit.getEntity(markerUUID) as? Marker)?.remove()
			}

			// REMOTE
			STORAGE_MODE == STORAGE_TYPE.DATABASE -> {
				RedisPublishAPI.deleteSoul(markerUUID)
			}
		}
	}


	/**
	 * Update Soul, when get a soul data copy from database and modify, then need update database to sync data.
	 */
	fun syncSoul() {
		// local soul and pdc mode do not need to sync to database
		if (isLocal || STORAGE_MODE == STORAGE_TYPE.PDC) return
		RedisPublishAPI.syncSoul(this)
	}


}