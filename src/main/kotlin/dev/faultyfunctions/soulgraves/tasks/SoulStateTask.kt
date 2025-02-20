package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.utils.SoulState
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class SoulStateTask(val soul: Soul) : BukkitRunnable() {
	override fun run() {

		// Offline Timer Freeze
		if (ConfigManager.offlineOwnerTimerFreeze && Bukkit.getPlayer(soul.ownerUUID) == null) {
			soul.timeLeft += 1 // Also Add a Second To expireTime
			return
		}

		// SET STATE , IF STATE IS EXPLODING MEAN MAYBE SOMEONE MAKE SOUL INSTANTLY EXPLOSION BY API
		if (soul.state != SoulState.EXPLODING) {
			if (soul.timeLeft > ConfigManager.timeUnstable)
				soul.state = SoulState.NORMAL
			else if (soul.timeLeft <= ConfigManager.timeUnstable && soul.timeLeft > 0)
				soul.state = SoulState.PANIC
			else
				soul.state = SoulState.EXPLODING
		}

		soul.timeLeft -= 1
	}
}