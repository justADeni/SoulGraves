package dev.faultyfunctions.soulgraves.managers

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import dev.faultyfunctions.soulgraves.SoulGraves
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.io.File
import java.io.IOException

object MessageManager {
	private lateinit var messageConfig: YamlDocument

	// MESSAGE VALUES
	var soulBurstComponent: Component? = null
	var soulBurstDropItemsComponent: Component? = null
	var soulBurstLoseItemsComponent: Component? = null
	var soulBurstNearbyComponent: Component? = null
	var soulCollectComponent: Component? = null
	var soulCollectOtherComponent: Component? = null
	var soulLimitExplodeComponent: Component? = null
	var soulGravesReloadComponent: Component? = null
	var commandBackNoSoulComponent: Component? = null
	var commandBackNoFundsComponent: Component? = null
	var commandBackSuccessFree: Component? = null
	var commandBackSuccessPaid: Component? = null

	fun loadMessages() {
		try {
			messageConfig = YamlDocument.create(File(SoulGraves.plugin.dataFolder, "messages.yml"),
				SoulGraves.plugin.getResource("messages.yml")!!,
				GeneralSettings.DEFAULT,
				LoaderSettings.builder().setAutoUpdate(true).build(),
				DumperSettings.DEFAULT,
				UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
			)

			messageConfig.update()
			messageConfig.save()
		} catch (_: IOException) {
			SoulGraves.plugin.logger.severe("Failed to load messages.yml! The plugin will now shut down.")
			Bukkit.getServer().pluginManager.disablePlugin(SoulGraves.plugin)
		}

		val miniMessage = MiniMessage.miniMessage()

		// LOAD VALUES
		if (messageConfig.getString("soul-burst") != "")
			soulBurstComponent = miniMessage.deserialize(messageConfig.getString("soul-burst").toString())
		if (messageConfig.getString("soul-burst-drop-items") != "")
			soulBurstDropItemsComponent = miniMessage.deserialize(messageConfig.getString("soul-burst-drop-items").toString())
		if (messageConfig.getString("soul-burst-lose-items") != "")
			soulBurstLoseItemsComponent = miniMessage.deserialize(messageConfig.getString("soul-burst-lose-items").toString())
		if (messageConfig.getString("soul-burst-nearby") != "")
			soulBurstNearbyComponent = miniMessage.deserialize(messageConfig.getString("soul-burst-nearby").toString())
		if (messageConfig.getString("soul-collect") != "")
			soulCollectComponent = miniMessage.deserialize(messageConfig.getString("soul-collect").toString())
		if (messageConfig.getString("soul-collect-other") != "")
			soulCollectOtherComponent = miniMessage.deserialize(messageConfig.getString("soul-collect-other").toString())
		if (messageConfig.getString("soul-limit-explode") != "")
			soulLimitExplodeComponent = miniMessage.deserialize(messageConfig.getString("soul-limit-explode")
				.replace("%max%", ConfigManager.maxSoulsPerPlayer.toString()))
		if (messageConfig.getString("soulgraves-reload") != "")
			soulGravesReloadComponent = miniMessage.deserialize(messageConfig.getString("soul-graves-reload").toString())
		if (messageConfig.getString("command-back-no-soul") != "")
			commandBackNoSoulComponent = miniMessage.deserialize(messageConfig.getString("command-back-no-soul").toString())
		if (messageConfig.getString("command-back-no-funds") != "")
			commandBackNoFundsComponent = miniMessage.deserialize(messageConfig.getString("command-back-no-funds").toString())
		if (messageConfig.getString("command-back-success-free") != "")
			commandBackSuccessFree = miniMessage.deserialize(messageConfig.getString("command-back-success-free").toString())
		if (messageConfig.getString("command-back-success-paid") != "")
			commandBackSuccessPaid = miniMessage.deserialize(messageConfig.getString("command-back-success-paid")
				.replace("%cost%", ConfigManager.teleportCost.toPlainString()))
	}
}