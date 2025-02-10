package dev.faultyfunctions.soulgraves.managers

import dev.faultyfunctions.soulgraves.SoulGraves
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.properties.Delegates

class SoundConfig() {
	var enabled by Delegates.notNull<Boolean>()
	val sounds = mutableListOf<String>()
	val volumes = mutableListOf<Float>()
	val pitches = mutableListOf<Float>()
}

object ConfigManager {
	private lateinit var file: File
	private val config: YamlConfiguration = YamlConfiguration()

	// CONFIG VALUES
	var permissionRequired by Delegates.notNull<Boolean>()
	var timeStable by Delegates.notNull<Int>()
	var timeUnstable by Delegates.notNull<Int>()
	var offlineOwnerTimerFreeze by Delegates.notNull<Boolean>()
	var notifyNearbyPlayers by Delegates.notNull<Boolean>()
	var notifyRadius by Delegates.notNull<Int>()
	var notifyOwnerPickup by Delegates.notNull<Boolean>()
	var xpPercentageOwner by Delegates.notNull<Double>()
	var xpPercentageOthers by Delegates.notNull<Double>()
	var xpPercentageBurst by Delegates.notNull<Double>()
	var ownerLocked by Delegates.notNull<Boolean>()
	var soulsDropItems by Delegates.notNull<Boolean>()
	var soulsDropXP by Delegates.notNull<Boolean>()
	val pickupSound = SoundConfig()
	val burstSound = SoundConfig()
	val notifyNearbySound = SoundConfig()
	val notifyOwnerBurstSound = SoundConfig()
	val notifyOwnerPickupSound = SoundConfig()
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
		permissionRequired = config.getBoolean("permission-required")
		timeStable = config.getInt("time-stable")
		timeUnstable = config.getInt("time-unstable")
		offlineOwnerTimerFreeze = config.getBoolean("offline-owner-timer-freeze")
		notifyNearbyPlayers = config.getBoolean("notify-nearby-players")
		notifyRadius = config.getInt("notify-radius")
		notifyOwnerPickup = config.getBoolean("notify-owner-pickup")
		xpPercentageOwner = config.getDouble("xp-percentage-owner")
		xpPercentageOthers = config.getDouble("xp-percentage-others")
		xpPercentageBurst = config.getDouble("xp-percentage-burst")
		ownerLocked = config.getBoolean("owner-locked")
		soulsDropItems = config.getBoolean("souls-drop-items")
		soulsDropXP = config.getBoolean("souls-drop-xp")
		pickupSound.enabled = config.getBoolean("pickup-sound.enabled")
		val pickupSounds = config.getStringList("pickup-sound.sounds")
		for (sound in pickupSounds) {
			val split = sound.split(",")
			pickupSound.sounds.add(split[0])
			pickupSound.volumes.add(split[1].toFloat())
			pickupSound.pitches.add(split[2].toFloat())
		}
		burstSound.enabled = config.getBoolean("burst-sound.enabled")
		val burstSounds = config.getStringList("burst-sound.sounds")
		for (sound in burstSounds) {
			val split = sound.split(",")
			burstSound.sounds.add(split[0])
			burstSound.volumes.add(split[1].toFloat())
			burstSound.pitches.add(split[2].toFloat())
		}
		notifyNearbySound.enabled = config.getBoolean("notify-nearby-sound.enabled")
		val notifyNearbySounds = config.getStringList("notify-nearby-sound.sounds")
		for (sound in notifyNearbySounds) {
			val split = sound.split(",")
			notifyNearbySound.sounds.add(split[0])
			notifyNearbySound.volumes.add(split[1].toFloat())
			notifyNearbySound.pitches.add(split[2].toFloat())
		}
		notifyOwnerBurstSound.enabled = config.getBoolean("notify-owner-burst-sound.enabled")
		val notifyOwnerBurstSounds = config.getStringList("notify-owner-burst-sound.sounds")
		for (sound in notifyOwnerBurstSounds) {
			val split = sound.split(",")
			notifyOwnerBurstSound.sounds.add(split[0])
			notifyOwnerBurstSound.volumes.add(split[1].toFloat())
			notifyOwnerBurstSound.pitches.add(split[2].toFloat())
		}
		notifyOwnerPickupSound.enabled = config.getBoolean("notify-owner-pickup-sound.enabled")
		val notifyOwnerPickupSounds = config.getStringList("notify-owner-pickup-sound.sounds")
		for (sound in notifyOwnerPickupSounds) {
			val split = sound.split(",")
			notifyOwnerPickupSound.sounds.add(split[0])
			notifyOwnerPickupSound.volumes.add(split[1].toFloat())
			notifyOwnerPickupSound.pitches.add(split[2].toFloat())
		}
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