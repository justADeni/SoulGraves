package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class SoulParticleTask(private val soul: Soul) : BukkitRunnable() {

    // Pre-compiled constants for better readability
    private companion object {
        const val VECTOR_MODE = 0
        const val Y_AXIS_IGNORE = 0.0
    }

    override fun run() {
        if (!ConfigManager.enableParticles) return
        val world = soul.location.world ?: return
        val owner = Bukkit.getPlayer(soul.ownerUUID)?.takeIf {
            it.isOnline &&
            !it.isDead &&
            it.gameMode != GameMode.SPECTATOR &&
            soul.location.world == world
        } ?: return

        when {
            ConfigManager.particlesFollowRadius > 0 -> handleRadiusFollow(world, owner)
            ConfigManager.particlesFollowRadius == 0.0 -> leaderParticle(owner)
        }
    }


    // Check Radius
    private fun handleRadiusFollow(world: World, owner: Player) {
        if (!world.isChunkLoaded(soul.location.chunk)) return

        world.getNearbyEntities(
            soul.location,
            ConfigManager.particlesFollowRadius,
            ConfigManager.particlesFollowRadius,
            ConfigManager.particlesFollowRadius
        )
            .filterIsInstance<Player>()
            .firstOrNull { it == owner }
            ?.let(::leaderParticle)
    }


    // Send Particles
    private fun leaderParticle(targetPlayer: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {

            // Calculate particle trajectory
            val eyeDirection = targetPlayer.eyeLocation.direction.apply { y = Y_AXIS_IGNORE }
            val spawnOrigin = targetPlayer.eyeLocation.add(eyeDirection.multiply(ConfigManager.particlesInitDistance))
            val directionVector = soul.location.toVector().subtract(spawnOrigin.toVector())

            // Generate randomized particles
            val random = Random()
            val particleCount = random.nextInt(1, ConfigManager.particleMaxAmount + 1)
            val speedVariation = ConfigManager.particleSpeedBound
            val baseSpeed = ConfigManager.particleSpeed

            repeat(particleCount) {
                val randomizedOffset = Triple(
                    random.nextDouble(-ConfigManager.particleOffsetBound, ConfigManager.particleOffsetBound),
                    random.nextDouble(-ConfigManager.particleOffsetBound, ConfigManager.particleOffsetBound),
                    random.nextDouble(-ConfigManager.particleOffsetBound, ConfigManager.particleOffsetBound)
                )

                // Sync for Bukkit API
                Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
                    targetPlayer.world.spawnParticle(
                        ConfigManager.particleType,
                        spawnOrigin.clone().add(randomizedOffset.first, randomizedOffset.second, randomizedOffset.third),
                        VECTOR_MODE,  // Use vector direction mode
                        directionVector.x,
                        directionVector.y,
                        directionVector.z,
                        random.nextDouble(baseSpeed - speedVariation, baseSpeed + speedVariation),
                        null,  // No extra data
                        true  // Force show to players
                    )
                })
            }
        })
    }

}

