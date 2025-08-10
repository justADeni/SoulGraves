package dev.faultyfunctions.soulgraves.commands

import dev.faultyfunctions.soulgraves.SoulGraves
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.orphan.OrphanCommand

class TeleportCommand : OrphanCommand {
	@Subcommand("tp")
	@Description("Teleports a player to their grave")
	@CommandPermission("soulgraves.command.teleport")
	fun teleportCommand(sender: Player, @Default("1") soulNumber: Int) {
		SoulGraves.plugin.logger.info("Teleport command executed by ${sender.name} for soul number $soulNumber")
	}
}