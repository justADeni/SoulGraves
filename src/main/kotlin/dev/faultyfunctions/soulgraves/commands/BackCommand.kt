package dev.faultyfunctions.soulgraves.commands

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.SoulGravesAPI
import dev.faultyfunctions.soulgraves.compatibilities.VaultHook
import dev.faultyfunctions.soulgraves.database.MessageAction
import dev.faultyfunctions.soulgraves.database.RedisDatabase
import dev.faultyfunctions.soulgraves.database.RedisPacket
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import dev.faultyfunctions.soulgraves.managers.SERVER_NAME
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.orphan.OrphanCommand
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.StorageType
import dev.faultyfunctions.soulgraves.messengers.PluginConnectMessenger
import java.math.BigDecimal

class BackCommand : OrphanCommand {
	@Subcommand("back")
	@Description("Returns a player to their last soul location")
	@CommandPermission("soulgraves.command.back")
	fun backCommand(sender: Player) {
		Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
			when(STORAGE_MODE) {
				StorageType.PDC -> {
					SoulGravesAPI.getPlayerSouls(sender.uniqueId).let { souls ->
						// CHECK IF PLAYER HAS SOULS
						val sorted = souls.sortedByDescending { it.expireTime }
						if (sorted.isEmpty()) {
							MessageManager.commandBackNoSoulComponent?.let {
								Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
									SoulGraves.plugin.adventure().player(sender).sendMessage(it)
								})
							}
							return@Runnable
						}
						// RUN LOGIC ON NEWEST SOUL
						val newestSoul = sorted.first()
						Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
							// IF THERE IS NO TELEPORT COST DEFINED IN THE CONFIG
							if (ConfigManager.teleportCost <= BigDecimal.ZERO) {
								MessageManager.commandBackSuccessFree?.let {
									SoulGraves.plugin.adventure().player(sender).sendMessage(it)
								}
								sender.teleport(newestSoul.location)
							} else {
								// OTHERWISE DOES THE PLAYER HAVE ENOUGH FUNDS?
								if (VaultHook.has(sender, ConfigManager.teleportCost)) {
									// REMOVE FUNDS, SEND MESSAGE, AND TELEPORT
									VaultHook.withdraw(sender, ConfigManager.teleportCost)
									MessageManager.commandBackSuccessPaid?.let {
										SoulGraves.plugin.adventure().player(sender).sendMessage(it)
									}
									sender.teleport(newestSoul.location)
								} else {
									// SEND NO FUNDS MESSAGE
									MessageManager.commandBackNoFundsComponent?.let {
										SoulGraves.plugin.adventure().player(sender).sendMessage(it)
									}
								}
							}
						})
					}
				}

				StorageType.CROSS_SERVER -> {
					val future = SoulGravesAPI.getPlayerSoulsCrossServer(sender.uniqueId)
					future.thenAccept { souls ->
						// CHECK IF PLAYER HAS SOULS
						//val sorted = souls.sortedByDescending { it.expireTime } // Maybe I don't need to sort?
						if (souls.isEmpty()) {
							MessageManager.commandBackNoSoulComponent?.let {
								Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
									SoulGraves.plugin.adventure().player(sender).sendMessage(it)
								})
							}
							return@thenAccept
						}
						// RUN LOGIC ON NEWEST SOUL
						val newestSoul = souls.first()
						Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
							// IF LOCAL SERVER
						    if (newestSoul.serverId == SERVER_NAME) {
								// IF THERE IS NO TELEPORT COST DEFINED IN THE CONFIG
						        if (ConfigManager.teleportCost <= BigDecimal.ZERO) {
						            MessageManager.commandBackSuccessFree?.let {
						                SoulGraves.plugin.adventure().player(sender).sendMessage(it)
						            }
						            sender.teleport(newestSoul.location)
						        } else {
						            VaultHook.let { vaultHook ->
										// IF PLAYER HAS ENOUGH FUNDS
						                if (vaultHook.has(sender, ConfigManager.teleportCost)) {
											// REMOVE FUNDS, SEND MESSAGE, AND TELEPORT
						                    vaultHook.withdraw(sender, ConfigManager.teleportCost)
						                    MessageManager.commandBackSuccessPaid?.let {
						                        SoulGraves.plugin.adventure().player(sender).sendMessage(it)
						                    }
						                    sender.teleport(newestSoul.location)
						                } else {
											// SEND NO FUNDS MESSAGE
						                    MessageManager.commandBackNoFundsComponent?.let {
						                        SoulGraves.plugin.adventure().player(sender).sendMessage(it)
						                    }
						                }
						            }
						        }
						    } else {
						        // SEND PLAYER TO CORRECT SERVER
						        PluginConnectMessenger.sendConnectMessage(sender, newestSoul.serverId)
								// SEND TELEPORT CROSS SERVER REDIS MESSAGE
								// WE WILL REMOVE FUNDS AND SEND A MESSAGE ONCE THE TELEPORT IS COMPLETE
						        val loc = newestSoul.location
						        val payload = "${sender.uniqueId}|${loc.world?.name}|${loc.x}|${loc.y}|${loc.z}"
						        RedisDatabase.instance.publish(RedisPacket(SERVER_NAME, MessageAction.TELEPORT_TO_SOUL, payload))
						    }
						})
					}
				}
			}
		})
	}
}