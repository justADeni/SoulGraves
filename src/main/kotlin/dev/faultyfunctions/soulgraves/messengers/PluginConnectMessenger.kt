package dev.faultyfunctions.soulgraves.messengers

import com.google.common.io.ByteStreams
import dev.faultyfunctions.soulgraves.SoulGraves
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object PluginConnectMessenger : PluginMessageListener {
	override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {	}

	fun sendConnectMessage(player: Player, serverName: String) {
		val messageOut = ByteStreams.newDataOutput()
		messageOut.writeUTF("Connect")
		messageOut.writeUTF(serverName)
		player.sendPluginMessage(SoulGraves.plugin, "BungeeCord", messageOut.toByteArray())
	}
}