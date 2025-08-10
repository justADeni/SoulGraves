package dev.faultyfunctions.soulgraves.commands

import org.bukkit.Bukkit
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

const val MAIN_COMMAND = "soulgraves"
const val ALIAS = "sg"

class BaseCommand {
	val aliases = arrayOf(MAIN_COMMAND, ALIAS)

	@Command(MAIN_COMMAND, ALIAS)
	@Description("The base command for SoulGraves")
	@CommandPermission("soulgraves.command")
	fun baseCommand(actor: BukkitCommandActor) {
		Bukkit.dispatchCommand(actor.sender(), "version SoulGraves")
	}
}