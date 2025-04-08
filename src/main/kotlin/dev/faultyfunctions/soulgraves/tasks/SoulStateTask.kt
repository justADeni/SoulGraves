package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.utils.SoulState
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class SoulStateTask(val soul: Soul) : BukkitRunnable() {
	override fun run() {

		// Offline Timer Freeze
		if (ConfigManager.offlineOwnerTimerFreeze && Bukkit.getPlayer(soul.ownerUUID)?.isOnline != true) { return }

		// SET STATE , IF STATE IS EXPLODING MEAN MAYBE SOMEONE MAKE SOUL INSTANTLY EXPLOSION BY API
		if (soul.state != SoulState.EXPLODING) {
			soul.state = when {
				(soul.timeLeft > ConfigManager.timeUnstable) -> SoulState.NORMAL
				(soul.timeLeft <= ConfigManager.timeUnstable) && (soul.timeLeft > 0) -> SoulState.PANIC
				else -> SoulState.EXPLODING
			}
		}

		soul.timeLeft -= 1
	}
}