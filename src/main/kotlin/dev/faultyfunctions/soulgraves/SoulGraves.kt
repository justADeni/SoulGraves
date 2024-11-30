package dev.faultyfunctions.soulgraves

import dev.faultyfunctions.soulgraves.commands.ReloadCommand
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.MessageManager
import com.jeff_media.morepersistentdatatypes.DataType
import dev.faultyfunctions.soulgraves.listeners.PlayerDeathListener
import dev.faultyfunctions.soulgraves.tasks.*
import dev.faultyfunctions.soulgraves.utils.Soul
import dev.faultyfunctions.soulgraves.utils.SpigotCompatUtils
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.entity.Marker
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import javax.annotation.Nonnull

val soulChunksKey = NamespacedKey(SoulGraves.plugin, "soul-chunks")
val soulKey = NamespacedKey(SoulGraves.plugin, "soul")
val soulOwnerKey = NamespacedKey(SoulGraves.plugin, "soul-owner")
val soulInvKey = NamespacedKey(SoulGraves.plugin, "soul-inv")
val soulXpKey = NamespacedKey(SoulGraves.plugin, "soul-xp")
val soulTimeLeftKey = NamespacedKey(SoulGraves.plugin, "soul-time-left")

class SoulGraves : JavaPlugin() {
	companion object {
		lateinit var plugin: SoulGraves
		var soulList: MutableList<Soul> = mutableListOf()
		val compat = SpigotCompatUtils
	}

	private lateinit var adventure: BukkitAudiences
	@Nonnull
	fun adventure(): BukkitAudiences {
		return this.adventure
	}

	override fun onEnable() {
		plugin = this
		plugin.adventure = BukkitAudiences.create(plugin)

		// LOAD CONFIG
		ConfigManager.loadConfig()
		MessageManager.loadMessages()

		initSouls()

		// LISTENERS
		server.pluginManager.registerEvents(PlayerDeathListener(), this)

		// COMMANDS
		getCommand("soulgraves")?.setExecutor(ReloadCommand())
		getCommand("soulgraves")?.tabCompleter = ReloadCommand()

		// TASKS
		SoulExplodeTask().runTaskTimer(this, 0, 20)
		SoulPickupTask().runTaskTimer(this, 0, 4)
		SoulRenderTask().runTaskTimer(this, 0, 1)
		SoulStateTask().runTaskTimer(this, 0, 20)
		SoulSoundTask().runTaskTimer(this, 0, 50)

		// SET UP BSTATS
		val pluginId = 23436
		val metrics = Metrics(this, pluginId)

		logger.info("Enabled!")
	}

	override fun onDisable() {
		this.adventure.close()

		logger.info("Disabled!")
	}

	private fun initSouls() {
		for (world in Bukkit.getWorlds()) {
			if (!world.persistentDataContainer.has(soulChunksKey)) {
				world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), mutableListOf<Long>())
			} else {
				// GET CHUNK KEY LIST AND MAKE SURE THEY ARE UNIQUE
				val chunkKeyList: List<Long>? = world.persistentDataContainer.get(soulChunksKey, DataType.asList(DataType.LONG))?.distinct()
				// RESET CHUNK LIST IN WORLD PDC
				world.persistentDataContainer.set(soulChunksKey, DataType.asList(DataType.LONG), mutableListOf<Long>())

				if (chunkKeyList != null) {
					for (chunkKey in chunkKeyList) {
						val chunk: Chunk = compat.getChunkAt(chunkKey, world)
						for (entity in chunk.entities) {
							if (entity.persistentDataContainer.has(soulKey)) {
								val markerEntity = entity as Marker
								val player = markerEntity.persistentDataContainer.get(soulOwnerKey, DataType.UUID)!!
								val inventory: MutableList<ItemStack?> = (markerEntity.persistentDataContainer.get(soulInvKey, DataType.ITEM_STACK_ARRAY)!!).toMutableList()
								val xp: Int = markerEntity.persistentDataContainer.get(soulXpKey, DataType.INTEGER)!!
								val timeLeft: Int = markerEntity.persistentDataContainer.get(soulTimeLeftKey, DataType.INTEGER)!!
								val soul = Soul(player, markerEntity.uniqueId, markerEntity.location, inventory, xp, timeLeft)
								soulList.add(soul)
							}
						}
					}
				}
			}
		}
	}
}
