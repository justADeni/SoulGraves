package dev.faultyfunctions.soulgraves.commands

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.SoulGraveAPI
import dev.faultyfunctions.soulgraves.compatibilities.VaultHook
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.orphan.OrphanCommand
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.StorageType
import java.math.BigDecimal

class BackCommand : OrphanCommand {
	@Subcommand("back")
	@Description("Returns a player to their last soul location")
	@CommandPermission("soulgraves.command.back")
	fun backCommand(sender: Player) {
		Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
			when(STORAGE_MODE) {
				StorageType.PDC -> {
					SoulGraveAPI.getPlayerSouls(sender.uniqueId)
						.let { souls ->
							val sorted = souls.sortedByDescending { it.expireTime }
							if (sorted.isEmpty()) {
								MessageManager.commandBackNoSoulComponent?.let {
									Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
										SoulGraves.plugin.adventure().player(sender).sendMessage(it)
									})
								}
								return@Runnable
							}
							val newestSoul = sorted.first()
							// Try to teleport
							Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
								if (ConfigManager.teleportCost <= BigDecimal.ZERO) {
									MessageManager.commandBackSuccessFree?.let {
										SoulGraves.plugin.adventure().player(sender).sendMessage(it)
									}
									sender.teleport(newestSoul.location)
									return@Runnable
								}
								VaultHook.instance.let { vaultHook ->
									if (vaultHook.has(sender, ConfigManager.teleportCost)) {
										vaultHook.withdraw( sender, ConfigManager.teleportCost)
										MessageManager.commandBackSuccessPaid?.let {
											SoulGraves.plugin.adventure().player(sender).sendMessage(it)
										}
										sender.teleport(newestSoul.location)
									} else {
										MessageManager.commandBackNoFundsComponent?.let {
											SoulGraves.plugin.adventure().player(sender).sendMessage(it)
										}
									}
								}
							})
						}
				}

				StorageType.CROSS_SERVER -> {
					TODO("Implement cross-server back command")
				}
			}
		})
	}
}