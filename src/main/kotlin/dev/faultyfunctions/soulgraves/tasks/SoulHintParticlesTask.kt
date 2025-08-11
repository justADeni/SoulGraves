package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.SoulGravesAPI
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class SoulHintParticlesTask(private val soul: Soul) : BukkitRunnable() {
    override fun run() {
        if (!ConfigManager.hintParticlesEnabled) return
        val world = soul.location.world ?: return
        val owner = Bukkit.getPlayer(soul.ownerUUID)?.takeIf {
            it.isOnline &&
            !it.isDead &&
            it.gameMode != GameMode.SPECTATOR &&
            soul.location.world == world
        } ?: return

        // MAKE SURE TO RUN BASED ON hintParticlesTrackedSoul SETTING
        var trackedSoul: Soul? = null
        if (ConfigManager.hintParticlesTrackedSoul == "OLDEST") {
            trackedSoul = SoulGravesAPI.getPlayerSouls(owner.uniqueId).minByOrNull { it.deathTime }
        } else if (ConfigManager.hintParticlesTrackedSoul == "NEWEST") {
            trackedSoul = SoulGravesAPI.getPlayerSouls(owner.uniqueId).maxByOrNull { it.deathTime }
        }
        if (trackedSoul == null || soul.markerUUID != trackedSoul.markerUUID) return

        when {
            ConfigManager.hintParticlesActivationRadius > 0 -> handleRadiusFollow(world, owner)
            ConfigManager.hintParticlesActivationRadius == 0 -> startHintParticles(owner)
        }
    }


    // Check Radius
    private fun handleRadiusFollow(world: World, owner: Player) {
        if (!world.isChunkLoaded(soul.location.chunk)) return

        world.getNearbyEntities(
            soul.location,
            ConfigManager.hintParticlesActivationRadius.toDouble(),
            ConfigManager.hintParticlesActivationRadius.toDouble(),
            ConfigManager.hintParticlesActivationRadius.toDouble()
        )
            .filterIsInstance<Player>()
            .firstOrNull { it == owner }
            ?.let(::startHintParticles)
    }

    private fun startHintParticles(targetPlayer: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(SoulGraves.plugin, Runnable {
            // CHECK IF PLAYER IS IN THE SAME WORLD AS SOUL
            if (targetPlayer.world != soul.location.world) { return@Runnable }

            // COMMON VALUES
            val eyeDirectionVector = targetPlayer.eyeLocation.direction.apply { y = 0.0 }
            val particleStartLocation = targetPlayer.eyeLocation.add(eyeDirectionVector.multiply(ConfigManager.hintParticlesStartDistance)).subtract(0.0, 1.0, 0.0)
            val soulDirectionVector = soul.location.toVector().subtract(particleStartLocation.toVector()).normalize().apply { y = 0.0 }
            val particleType = Particle.valueOf(ConfigManager.hintParticlesParticleType)

            val trailRunnable = Runnable {
                // ADD VERTICAL OFFSET FOR CLIFF VISIBILITY
                if (particleStartLocation.block.type.isSolid) {
                    particleStartLocation.add(0.0, 1.0, 0.0)
                }

                // SPAWN PARTICLE
                targetPlayer.spawnParticle(particleType, particleStartLocation, 1, 0.2, 0.0, 0.2, 0.0)
                particleStartLocation.add(soulDirectionVector.clone().multiply(1.0 / ConfigManager.hintParticlesTrailDensity))
            }

            if (targetPlayer.location.distance(soul.location) < 8) { return@Runnable }

            when (ConfigManager.hintParticlesMode) {
                "WANDER" -> {
                    repeat(ConfigManager.hintParticlesWanderCount) {
                        val startX = particleStartLocation.x + Random.nextDouble(-1.0, 3.0)
                        val startY = particleStartLocation.y + Random.nextDouble(-1.0, 3.0)
                        val startZ = particleStartLocation.z + Random.nextDouble(-1.0, 3.0)
                        val startLocation = Location(targetPlayer.world, startX, startY, startZ)

                        // ADD VERTICAL OFFSET FOR CLIFF VISIBILITY
                        if (startLocation.block.type.isSolid) {
                            startLocation.add(0.0, 1.0, 0.0)
                        }

                        val speed = Random.nextDouble(ConfigManager.hintParticlesWanderMinSpeed, ConfigManager.hintParticlesWanderMaxSpeed)

                        targetPlayer.spawnParticle(particleType, startLocation, 0, soulDirectionVector.x, soulDirectionVector.y, soulDirectionVector.z, speed)
                    }
                }
                "TRAIL" -> {
                    var tickDelay = 1L
                    val repeatAmount = ConfigManager.hintParticlesTrailLength * ConfigManager.hintParticlesTrailDensity
                    repeat(repeatAmount) {
                        Bukkit.getScheduler().runTaskLater(SoulGraves.plugin, trailRunnable, tickDelay)
                        tickDelay += 1
                    }
                }
            }


        })
    }
}

