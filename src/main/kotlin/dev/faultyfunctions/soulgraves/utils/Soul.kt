package dev.faultyfunctions.soulgraves.utils

import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import dev.faultyfunctions.soulgraves.api.RedisPublishAPI
import dev.faultyfunctions.soulgraves.api.event.SoulDeleteEvent
import dev.faultyfunctions.soulgraves.database.*
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
	val markerUUID: UUID,
	val location: Location,
	var inventory: MutableList<ItemStack?>,
	var xp: Int,

	val deathTime: Long,
	expireTime: Long,
	timeLeft: Int, // Second
	var freezeTime: Long = 0, // IF Config offlineOwnerTimerFreeze is True, Will Record Owner Logout Time

	val serverId: String = SERVER_NAME,
	val isLocal: Boolean = serverId == SERVER_NAME
) {
	private var _expireTime: Long = expireTime
	private var _timeLeft: Int = timeLeft

	var expireTime: Long
		get() = _expireTime
		set(value) {
			_expireTime = value
			_timeLeft = ((value - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
		}

	var timeLeft: Int
		get() = if (isLocal) _timeLeft else ((_expireTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
		set(value) {
			_timeLeft = value.coerceAtLeast(0)
			_expireTime = System.currentTimeMillis() + _timeLeft * 1000L
		}

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
			SoulGraves.soulList.add(soul)
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
			expireTime: Long,
			freezeTime: Long
		) = Soul(
			markerUUID = markerUUID,
			ownerUUID = ownerUUID,
			location = location,
			inventory = inventory,
			xp = xp,

			deathTime = deathTime,
			expireTime = expireTime,
			timeLeft = ((expireTime - deathTime) / 1000).toInt(),
			freezeTime = freezeTime,

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
			freezeTime: Long
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
				freezeTime = freezeTime,

				isLocal = true
			)
			// Init Soul
			SoulGraves.soulList.add(soul)
			soul.startTasks()
			return soul
		}
	}

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
		if (!isValid()) delete()

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
	 * Save to Database, Remember Save After Modified Soul
	 */
	fun saveData() {
		// If not local soul
		if (!isLocal) return
		if (!isValid()) delete()

		// Save
		when (STORAGE_MODE) {
			// PDC
			STORAGE_TYPE.PDC -> {
				PDCDatabase.instance.saveSoul(this)
			}
			// DATABASE
			STORAGE_TYPE.DATABASE -> {
				Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
					MySQLDatabase.instance.saveSoul(this)
				})
			}
		}
	}


	fun isValid(): Boolean {
		// If world is not exist, entity is not exist
		val world = location.world ?: return false
		world.loadChunk(location.chunk)
		Bukkit.getEntity(markerUUID) ?: return false
		return true
	}


	/**
	 * Make Soul Explode Now, Will Drop Exp And Items.
	 */
	fun explode(): Boolean {
		when {
			// Local
			isLocal -> {
				this.state = SoulState.EXPLODING
				this.implosion = true
				return true
			}

			// REMOTE
			STORAGE_MODE == STORAGE_TYPE.DATABASE -> {
				RedisPublishAPI.explodeSoul(markerUUID)
				return true
			}
		}

		return false
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
				PDCDatabase.instance.deleteSoul(this)
				// REMOVE ENTITY
				location.world?.loadChunk(location.chunk).apply {
					(Bukkit.getEntity(markerUUID) as? Marker)?.remove()
				}
				// REMOVE RECORD
				SoulGraves.soulList.remove(this)
				// CALL EVENT
				val event = SoulDeleteEvent(this)
				Bukkit.getPluginManager().callEvent(event)
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
				location.world?.loadChunk(location.chunk).apply {
					(Bukkit.getEntity(markerUUID) as? Marker)?.remove()
				}
				// REMOVE RECORD
				SoulGraves.soulList.remove(this)
				// CALL EVENT
				val event = SoulDeleteEvent(this)
				Bukkit.getPluginManager().callEvent(event)
			}

			// REMOTE
			STORAGE_MODE == STORAGE_TYPE.DATABASE -> {
				RedisPublishAPI.deleteSoul(markerUUID)
			}
		}
	}
}