package dev.faultyfunctions.soulgraves.utils

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.CrossServerManager
import dev.faultyfunctions.soulgraves.soulKey
import dev.faultyfunctions.soulgraves.tasks.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Marker
import org.bukkit.inventory.ItemStack
import java.util.UUID

enum class SoulState {
	NORMAL, PANIC, EXPLODING
}

class Soul(
	val ownerUUID: UUID,
	val markerUUID: UUID,
	val location: Location,

	var inventory:MutableList<ItemStack?>,
	var xp: Int,
	var timeLeft: Int
) {
	val serverId: String = CrossServerManager.serverId
	val createTime: Long = System.currentTimeMillis()
	var state: Enum<SoulState> = SoulState.NORMAL
	var implosion: Boolean = false

	private val stateTask: SoulStateTask = SoulStateTask(this)
	private val soundTask: SoulSoundTask = SoulSoundTask(this)
	private val renderTask: SoulRenderTask = SoulRenderTask(this)
	private val pickupTask: SoulPickupTask = SoulPickupTask(this)
	private val explodeTask: SoulExplodeTask = SoulExplodeTask(this)

	// Start to Run Tasks
	fun start() {
		stateTask.runTaskTimer(SoulGraves.plugin, 0, 20)
		soundTask.runTaskTimer(SoulGraves.plugin, 0, 50)
		renderTask.runTaskTimer(SoulGraves.plugin, 0, 2)
		pickupTask.runTaskTimer(SoulGraves.plugin, 0, 5)
		explodeTask.runTaskTimer(SoulGraves.plugin, 0, 20)
	}

	// Freeze
	fun freeze() {
		stateTask.cancel()
	}

	// Delete
	fun delete() {
		stateTask.cancel()
		soundTask.cancel()
		renderTask.cancel()
		pickupTask.cancel()
		explodeTask.cancel()
		(Bukkit.getEntity(markerUUID) as Marker).remove()
	}















}