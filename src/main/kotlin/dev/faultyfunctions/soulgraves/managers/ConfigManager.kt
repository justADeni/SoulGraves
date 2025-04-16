package dev.faultyfunctions.soulgraves.managers

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.block.Comments
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.MergeRule
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import dev.dejvokep.boostedyaml.utils.format.NodeRole
import dev.faultyfunctions.soulgraves.SoulGraves
import org.bukkit.Bukkit
import org.bukkit.Particle
import java.io.File
import java.io.IOException
import kotlin.properties.Delegates

class SoundConfig() {
	var enabled by Delegates.notNull<Boolean>()
	val sounds = mutableListOf<String>()
	val volumes = mutableListOf<Float>()
	val pitches = mutableListOf<Float>()

	fun clear() {
		sounds.clear()
		volumes.clear()
		pitches.clear()
	}
}

object ConfigManager {
	private lateinit var config: YamlDocument

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
	var maxSoulsPerPlayer by Delegates.notNull<Int>()
	var soulsDropItems by Delegates.notNull<Boolean>()
	var soulsDropXP by Delegates.notNull<Boolean>()
	val pickupSound = SoundConfig()
	val burstSound = SoundConfig()
	val notifyNearbySound = SoundConfig()
	val notifyOwnerBurstSound = SoundConfig()
	val notifyOwnerPickupSound = SoundConfig()
	lateinit var disabledWorlds: List<String>
	// PARTICLES CONFIG
	var hintParticlesEnabled by Delegates.notNull<Boolean>()
	var hintParticlesActivationRadius by Delegates.notNull<Int>()
	var hintParticlesTrackedSoul by Delegates.notNull<String>()
	var hintParticlesParticleType by Delegates.notNull<String>()
	var hintParticlesStartDistance by Delegates.notNull<Int>()
	var hintParticlesMode by Delegates.notNull<String>()
	var hintParticlesTrailLength by Delegates.notNull<Int>()
	var hintParticlesTrailDensity by Delegates.notNull<Int>()
	var hintParticlesWanderCount by Delegates.notNull<Int>()
	var hintParticlesWanderMinSpeed by Delegates.notNull<Double>()
	var hintParticlesWanderMaxSpeed by Delegates.notNull<Double>()

	fun loadConfig() {
		try {
			config = YamlDocument.create(File(SoulGraves.plugin.dataFolder, "config.yml"),
				SoulGraves.plugin.getResource("config.yml")!!,
				GeneralSettings.DEFAULT,
				LoaderSettings.builder().setAutoUpdate(true).build(),
				DumperSettings.DEFAULT,
				UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
			)

			config.update()
			config.save()
		} catch (e: IOException) {
			SoulGraves.plugin.logger.severe("Failed to load config.yml! The plugin will now shut down.")
			Bukkit.getServer().pluginManager.disablePlugin(SoulGraves.plugin)
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
		maxSoulsPerPlayer = config.getInt("max-souls-per-player")
		soulsDropItems = config.getBoolean("souls-drop-items")
		soulsDropXP = config.getBoolean("souls-drop-xp")
		pickupSound.enabled = config.getBoolean("pickup-sound.enabled")
		pickupSound.clear()
		val pickupSounds = config.getStringList("pickup-sound.sounds")
		for (sound in pickupSounds) {
			val split = sound.split(",")
			pickupSound.sounds.add(split[0])
			pickupSound.volumes.add(split[1].toFloat())
			pickupSound.pitches.add(split[2].toFloat())
		}
		burstSound.enabled = config.getBoolean("burst-sound.enabled")
		burstSound.clear()
		val burstSounds = config.getStringList("burst-sound.sounds")
		for (sound in burstSounds) {
			val split = sound.split(",")
			burstSound.sounds.add(split[0])
			burstSound.volumes.add(split[1].toFloat())
			burstSound.pitches.add(split[2].toFloat())
		}
		notifyNearbySound.enabled = config.getBoolean("notify-nearby-sound.enabled")
		notifyNearbySound.clear()
		val notifyNearbySounds = config.getStringList("notify-nearby-sound.sounds")
		for (sound in notifyNearbySounds) {
			val split = sound.split(",")
			notifyNearbySound.sounds.add(split[0])
			notifyNearbySound.volumes.add(split[1].toFloat())
			notifyNearbySound.pitches.add(split[2].toFloat())
		}
		notifyOwnerBurstSound.enabled = config.getBoolean("notify-owner-burst-sound.enabled")
		notifyOwnerBurstSound.clear()
		val notifyOwnerBurstSounds = config.getStringList("notify-owner-burst-sound.sounds")
		for (sound in notifyOwnerBurstSounds) {
			val split = sound.split(",")
			notifyOwnerBurstSound.sounds.add(split[0])
			notifyOwnerBurstSound.volumes.add(split[1].toFloat())
			notifyOwnerBurstSound.pitches.add(split[2].toFloat())
		}
		notifyOwnerPickupSound.enabled = config.getBoolean("notify-owner-pickup-sound.enabled")
		notifyOwnerPickupSound.clear()
		val notifyOwnerPickupSounds = config.getStringList("notify-owner-pickup-sound.sounds")
		for (sound in notifyOwnerPickupSounds) {
			val split = sound.split(",")
			notifyOwnerPickupSound.sounds.add(split[0])
			notifyOwnerPickupSound.volumes.add(split[1].toFloat())
			notifyOwnerPickupSound.pitches.add(split[2].toFloat())
		}
		disabledWorlds = config.getStringList("disabled-worlds")
		// PARTICLES CONFIG
		hintParticlesEnabled = config.getBoolean("hint-particles.enabled")
		hintParticlesActivationRadius = config.getInt("hint-particles.activation-radius")
		hintParticlesTrackedSoul = config.getString("hint-particles.tracked-soul")
		hintParticlesParticleType = config.getString("hint-particles.particle-type")
		hintParticlesStartDistance = config.getInt("hint-particles.start-distance")
		hintParticlesMode = config.getString("hint-particles.mode")
		hintParticlesTrailLength = config.getInt("hint-particles.trail.length")
		hintParticlesTrailDensity = config.getInt("hint-particles.trail.density")
		hintParticlesWanderCount = config.getInt("hint-particles.wander.count")
		hintParticlesWanderMinSpeed = config.getDouble("hint-particles.wander.min-speed")
		hintParticlesWanderMaxSpeed = config.getDouble("hint-particles.wander.max-speed")
	}
}