package dev.faultyfunctions.soulgraves

import dev.faultyfunctions.soulgraves.commands.BackCommand
import dev.faultyfunctions.soulgraves.commands.BaseCommand
import dev.faultyfunctions.soulgraves.commands.ReloadCommand
import dev.faultyfunctions.soulgraves.compatibilities.EcoEnchantsHook
import dev.faultyfunctions.soulgraves.compatibilities.ExcellentEnchantsHook
import dev.faultyfunctions.soulgraves.compatibilities.VaneEnchantmentsHook
import dev.faultyfunctions.soulgraves.compatibilities.VaultHook
import dev.faultyfunctions.soulgraves.compatibilities.WorldGuardHook
import dev.faultyfunctions.soulgraves.database.MySQLDatabase
import dev.faultyfunctions.soulgraves.database.PDCDatabase
import dev.faultyfunctions.soulgraves.database.RedisDatabase
import dev.faultyfunctions.soulgraves.listeners.PlayerConnectionEvent
import dev.faultyfunctions.soulgraves.listeners.PlayerDeathListener
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.DatabaseManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import dev.faultyfunctions.soulgraves.managers.STORAGE_MODE
import dev.faultyfunctions.soulgraves.managers.StorageType
import dev.faultyfunctions.soulgraves.utils.Soul
import dev.faultyfunctions.soulgraves.utils.SpigotCompatUtils
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.orphan.Orphans
import java.util.concurrent.CopyOnWriteArrayList

class SoulGraves : JavaPlugin() {
	companion object {
		lateinit var plugin: SoulGraves
		var soulList = CopyOnWriteArrayList<Soul>()
	}

	private lateinit var adventure: BukkitAudiences
	fun adventure(): BukkitAudiences {
		return this.adventure
	}

	override fun onLoad() {
		// Compatibilities
		if (SpigotCompatUtils.isPluginLoaded("WorldGuard")) {
			WorldGuardHook.instance.registerFlags()
		}
	}

	override fun onEnable() {
		plugin = this
		plugin.adventure = BukkitAudiences.create(plugin)

		// LOAD CONFIG
		ConfigManager.loadConfig()
		MessageManager.loadMessages()
		DatabaseManager.loadConfig()

		// INIT SOULS
		when (STORAGE_MODE) {
			// PDC
			StorageType.PDC  -> {
				PDCDatabase.instance.initSouls()
			}
			// MYSQL + REDIS
			StorageType.CROSS_SERVER -> {
				MySQLDatabase.instance
				RedisDatabase.instance
			}
			// OTHER NOT VALID MODE
			else -> {
				logger.severe("STORAGE MODE ERROR! PLUGIN WILL NOW DISABLE ITSELF!")
				server.pluginManager.disablePlugin(this)
			}
		}

		// LISTENERS
		server.pluginManager.registerEvents(PlayerDeathListener(), this)
		server.pluginManager.registerEvents(PlayerConnectionEvent(), this)

		// COMPATIBILITY HOOKS
		if (SpigotCompatUtils.isPluginLoaded("WorldGuard")) { WorldGuardHook.instance.init() }
		if (SpigotCompatUtils.isPluginLoaded("vane-enchantments")) { VaneEnchantmentsHook.instance.init() }
		if (SpigotCompatUtils.isPluginLoaded("ExcellentEnchants")) { ExcellentEnchantsHook.instance.init() }
		if (SpigotCompatUtils.isPluginLoaded("EcoEnchants")) { EcoEnchantsHook.instance.init() }
		if (SpigotCompatUtils.isPluginLoaded("Vault")) { VaultHook.instance.init() }

		// COMMANDS
		val lamp = BukkitLamp.builder(this).build()
		val baseCommand = BaseCommand()
		lamp.register(baseCommand)
		lamp.register(Orphans.path(*baseCommand.aliases).handler(ReloadCommand()))
		lamp.register(Orphans.path(*baseCommand.aliases).handler(BackCommand()))

		// SET UP BSTATS
		val pluginId = 23436
		val metrics = Metrics(this, pluginId)

		logger.info("Enabled!")
	}

	override fun onDisable() {
		this.adventure.close()
		if (STORAGE_MODE == StorageType.CROSS_SERVER) { RedisDatabase.instance.shutdown() }
		logger.info("Disabled!")
	}
}
