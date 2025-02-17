package dev.faultyfunctions.soulgraves.utils

import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.*
import dev.faultyfunctions.soulgraves.api.RedisPublishAPI
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.managers.ConfigManager
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

class Soul(
	val ownerUUID: UUID,
	var markerUUID: UUID?,
	val location: Location,
	var inventory:MutableList<ItemStack?>,
	var xp: Int,
	var timeLeft: Int,
	val serverId: String = ConfigManager.serverName,
	val expireTime: Long = System.currentTimeMillis() + ((ConfigManager.timeStable + ConfigManager.timeUnstable) * 1000)
) {
	var state: Enum<SoulState> = SoulState.NORMAL
	var implosion: Boolean = false

	private val explodeTask: SoulExplodeTask = SoulExplodeTask(this)
	private val particleTask: SoulParticleTask = SoulParticleTask(this)
	private val pickupTask: SoulPickupTask = SoulPickupTask(this)
	private val renderTask: SoulRenderTask = SoulRenderTask(this)
	private val soundTask: SoulSoundTask = SoulSoundTask(this)
	private val stateTask: SoulStateTask = SoulStateTask(this)

	init {
		if (serverId == ConfigManager.serverName) {
			this.markerUUID = markerUUID ?: spawnMarker()?.uniqueId // Spawn Marker If Marker Not Exist
			startTasks()
		}
	}


	/**
	 * Start Soul Tasks.
	 */
	private fun startTasks() {
		markerUUID ?: this.delete() // IF SOUL DO NOT HAVE UUID MEAN WORLD IS NOT EXIST, DATA WILL REMOVE
		explodeTask.runTaskTimer(SoulGraves.plugin, 0, 20)
		particleTask.runTaskTimer(SoulGraves.plugin, 0, 50)
		pickupTask.runTaskTimer(SoulGraves.plugin, 0, 4)
		renderTask.runTaskTimer(SoulGraves.plugin, 0, 1)
		soundTask.runTaskTimer(SoulGraves.plugin, 0, 50)
		stateTask.runTaskTimer(SoulGraves.plugin, 0, 20)
	}


	/**
	 * Spawn a Marker Entity upon Soul Created.
	 */
	private fun spawnMarker(): Entity? {
		location.chunk.load()
		if (location.world != null) {
			val marker = location.world!!.spawnEntity(location, EntityType.MARKER) as Marker
			marker.isPersistent = true
			marker.isSilent = true
			marker.isInvulnerable = true
			// STORE DATA
			marker.persistentDataContainer.set(soulKey, DataType.BOOLEAN, true)
			marker.persistentDataContainer.set(soulOwnerKey, DataType.UUID, ownerUUID)
			marker.persistentDataContainer.set(soulInvKey, DataType.ITEM_STACK_ARRAY, inventory.toTypedArray())
			marker.persistentDataContainer.set(soulXpKey, DataType.INTEGER, xp)
			marker.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, timeLeft)
			return marker
		}
		return null
	}


	/**
	 * Make Soul Explode Now, Will Drop Exp And Items.
	 */
	fun explodeNow() {
		if (serverId == ConfigManager.serverName) {
			this.state = SoulState.EXPLODING
			this.implosion = true
		} else {
			markerUUID?.let { RedisPublishAPI.explodeSoul(it) }
		}
	}


	/**
	 * Delete Soul, Stop All Task of Soul, Will Drop Nothing.
	 */
	fun delete() {
		if (serverId == ConfigManager.serverName) {
			explodeTask.cancel()
			particleTask.cancel()
			pickupTask.cancel()
			renderTask.cancel()
			soundTask.cancel()
			stateTask.cancel()

			markerUUID?.let {
				location.chunk.load()
				(Bukkit.getEntity(it) as Marker).remove()
			}
			SoulGraves.soulList.remove(this)
			Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable { MySQLDatabase.instance.deleteSoul(this) })
		} else {
			markerUUID?.let { RedisPublishAPI.deleteSoul(it) }
		}
	}

}