package dev.faultyfunctions.soulgraves.commands

import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.orphan.OrphanCommand

class CountCommand : OrphanCommand {
	@Subcommand("count")
	@Description("Shows the numbers of active souls a player has")
	fun countCommand() {

	}
}