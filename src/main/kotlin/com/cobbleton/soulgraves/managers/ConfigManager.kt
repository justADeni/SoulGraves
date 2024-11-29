package com.cobbleton.soulgraves.managers

import com.cobbleton.soulgraves.SoulGraves
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.properties.Delegates

object ConfigManager {
	private lateinit var file: File
	private val config: YamlConfiguration = YamlConfiguration()

	// CONFIG VALUES
	var timeStable by Delegates.notNull<Int>()
	var timeUnstable by Delegates.notNull<Int>()
	var notifyNearbyPlayers by Delegates.notNull<Boolean>()
	var notifyRadius by Delegates.notNull<Int>()
	var xpPercentageOwner by Delegates.notNull<Double>()
	var xpPercentageOthers by Delegates.notNull<Double>()
	var xpPercentageBurst by Delegates.notNull<Double>()
	var ownerLocked by Delegates.notNull<Boolean>()
	var soulsDropItems by Delegates.notNull<Boolean>()
	var soulsDropXP by Delegates.notNull<Boolean>()
	lateinit var disabledWorlds: List<String>

	fun loadConfig() {
		// GRAB FILE
		file = File(SoulGraves.plugin.dataFolder, "config.yml")

		// CREATE FILE IF IT DOESN'T EXIST
		if (!file.exists()) {
			SoulGraves.plugin.saveResource("config.yml", false)
		}

		// MAKE SURE WE KEEP COMMENTS
		config.options().parseComments(true)

		// LOAD FILE
		try {
			config.load(file)
		} catch (e: Exception) {
			e.printStackTrace()
		}

		// LOAD VALUES
		timeStable = config.getInt("time-stable")
		timeUnstable = config.getInt("time-unstable")
		notifyNearbyPlayers = config.getBoolean("notify-nearby-players")
		notifyRadius = config.getInt("notify-radius")
		xpPercentageOwner = config.getDouble("xp-percentage-owner")
		xpPercentageOthers = config.getDouble("xp-percentage-others")
		xpPercentageBurst = config.getDouble("xp-percentage-burst")
		ownerLocked = config.getBoolean("owner-locked")
		soulsDropItems = config.getBoolean("souls-drop-items")
		soulsDropXP = config.getBoolean("souls-drop-xp")
		disabledWorlds = config.getStringList("disabled-worlds")
	}

//	fun saveConfig() {
//		try {
//			config.save(file)
//		} catch (e: Exception) {
//			e.printStackTrace()
//		}
//	}
}