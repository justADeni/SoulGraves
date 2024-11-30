package dev.faultyfunctions.soulgraves.utils

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.util.UUID

enum class SoulState {
	NORMAL, PANIC, EXPLODING
}

data class Soul(val ownerUUID: UUID, val markerUUID: UUID, val location: Location, val inventory:MutableList<ItemStack?>, val xp: Int, var timeLeft: Int) {
	var state: Enum<SoulState> = SoulState.NORMAL
	var implosion: Boolean = false
}