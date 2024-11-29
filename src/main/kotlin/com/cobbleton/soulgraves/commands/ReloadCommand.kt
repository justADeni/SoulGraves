package com.cobbleton.soulgraves.commands
import com.cobbleton.soulgraves.managers.ConfigManager
import com.cobbleton.soulgraves.managers.MessageManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class ReloadCommand: CommandExecutor, TabExecutor {
	override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String>? {
		val completionList: MutableList<String> = mutableListOf()

		if (args?.size == 1) {
			completionList.add("reload")
			return completionList
		}

		return null
	}

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
		if (args?.size == 0) {
			Bukkit.dispatchCommand(sender, "version SoulGraves")
		}

		if (args?.size == 1) {
			if (args[0].equals("reload", ignoreCase = true)) {
				ConfigManager.loadConfig()
				MessageManager.loadMessages()
				sender.sendMessage("Config reloaded!")
			}
		}

		return true
	}
}