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
	var soulBurstComponent: Component? = null
	var soulBurstDropItemsComponent: Component? = null
	var soulBurstLoseItemsComponent: Component? = null
	var soulBurstNearbyComponent: Component? = null
	var soulCollectComponent: Component? = null
	var soulCollectOtherComponent: Component? = null

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
		if (config.getString("soul-burst") != "")
			soulBurstComponent = miniMessage.deserialize(config.getString("soul-burst").toString())
		if (config.getString("soul-burst-drop-items") != "")
			soulBurstDropItemsComponent = miniMessage.deserialize(config.getString("soul-burst-drop-items").toString())
		if (config.getString("soul-burst-lose-items") != "")
			soulBurstLoseItemsComponent = miniMessage.deserialize(config.getString("soul-burst-lose-items").toString())
		if (config.getString("soul-burst-nearby") != "")
			soulBurstNearbyComponent = miniMessage.deserialize(config.getString("soul-burst-nearby").toString())
		if (config.getString("soul-collect") != "")
			soulCollectComponent = miniMessage.deserialize(config.getString("soul-collect").toString())
		if (config.getString("soul-collect-other") != "")
			soulCollectOtherComponent = miniMessage.deserialize(config.getString("soul-collect-other").toString())
	}
}