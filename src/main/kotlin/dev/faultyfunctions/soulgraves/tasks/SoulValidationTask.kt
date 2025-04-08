package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.utils.Soul
import dev.faultyfunctions.soulgraves.utils.SoulState
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class SoulValidationTask(val soul: Soul) : BukkitRunnable() {

    override fun run() {
        with(soul) {
            // IF WORLD IS NOT EXIST
            val world = location.world ?: return delete()
            // IF CHUNK IS NOT LOAD, TASK WILL RETURN
            if (!world.isChunkLoaded(location.chunk) || state == SoulState.EXPLODING) return
            // IF MARKER IS NOT EXIST
            markerUUID.let { Bukkit.getEntity(it) } ?: return delete()
        }
    }

}