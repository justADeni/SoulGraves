package dev.faultyfunctions.soulgraves.managers

import dev.faultyfunctions.soulgraves.SoulGraves
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object MessageManager {
	private lateinit var file: File
	private val config: YamlConfiguration = YamlConfiguration()

	// MESSAGE VALUES
	lateinit var soulBurstComponent: Component
	lateinit var soulBurstDropItemsComponent: Component
	lateinit var soulBurstLoseItemsComponent: Component
	lateinit var soulBurstNearbyComponent: Component
	lateinit var soulCollectComponent: Component
	lateinit var soulCollectOtherComponent: Component

	fun loadMessages() {
		// GRAB FILE
		file = File(SoulGraves.plugin.dataFolder, "messages.yml")

		// CREATE FILE IF IT DOESN'T EXIST
		if (!file.exists()) {
			SoulGraves.plugin.saveResource("messages.yml", false)
		}

		// MAKE SURE WE KEEP COMMENTS
		config.options().parseComments(true)

		// LOAD FILE
		try {
			config.load(file)
		} catch (e: Exception) {
			e.printStackTrace()
		}

		val miniMessage = MiniMessage.miniMessage()

		// LOAD VALUES
		soulBurstComponent = miniMessage.deserialize(config.getString("soul-burst").toString())
		soulBurstDropItemsComponent = miniMessage.deserialize(config.getString("soul-burst-drop-items").toString())
		soulBurstLoseItemsComponent = miniMessage.deserialize(config.getString("soul-burst-lose-items").toString())
		soulBurstNearbyComponent = miniMessage.deserialize(config.getString("soul-burst-nearby").toString())
		soulCollectComponent = miniMessage.deserialize(config.getString("soul-collect").toString())
		soulCollectOtherComponent = miniMessage.deserialize(config.getString("soul-collect-other").toString())
	}
}