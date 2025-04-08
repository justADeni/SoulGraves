package dev.faultyfunctions.soulgraves.utils

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World

object SpigotCompatUtils {
	fun getChunkKey(chunk: Chunk): Long {
		return (chunk.z.toLong() shl 32) or (chunk.x.toLong() and 0xffffffffL)
	}

	fun getChunkAt(chunkKey: Long, world: World): Chunk {
		val x: Int = ((chunkKey shl 32) shr 32).toInt()
		val z: Int = (chunkKey shr 32).toInt()

		return world.getChunkAt(x, z)
	}

	fun calculateTotalExperiencePoints(level: Int): Int {
		return when {
			(level <= 16) -> (level * level) + (6 * level)
			(level <= 31) -> (2.5 * level * level - 40.5 * level + 360).toInt()
			else -> (4.5 * level * level - 162.5 * level + 2220).toInt()
		}
	}

	fun isPluginLoaded(plugin: String): Boolean {
		return Bukkit.getPluginManager().getPlugin(plugin) != null
	}
}