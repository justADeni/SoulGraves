package dev.faultyfunctions.soulgraves.commands

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.orphan.OrphanCommand

class ReloadCommand : OrphanCommand {
	@Subcommand("reload")
	@Description("Reloads the SoulGraves configuration and messages")
	@CommandPermission("soulgraves.command.reload")
	fun reloadCommand(actor: BukkitCommandActor) {
		ConfigManager.loadConfig()
		MessageManager.loadMessages()
		val reloadComponent = MessageManager.soulGravesReloadComponent
		if (reloadComponent != null) {
			if (actor.isPlayer) {
				actor.asPlayer()?.let { SoulGraves.plugin.adventure().player(it) }?.sendMessage(reloadComponent)
			} else {
				SoulGraves.plugin.adventure().console().sendMessage(reloadComponent)
			}
		}
	}
}