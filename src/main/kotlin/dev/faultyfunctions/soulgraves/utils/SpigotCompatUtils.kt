package dev.faultyfunctions.soulgraves.utils

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
}