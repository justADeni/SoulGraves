package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.utils.Soul
import dev.faultyfunctions.soulgraves.utils.SoulState
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.min
import kotlin.math.sin

class SoulRenderTask(val soul: Soul) : BukkitRunnable() {
	private val particleLocation = Location(null, 0.0, 0.0, 0.0)

	override fun run() {
		val world = soul.location.world ?: return
		if (!world.isChunkLoaded(soul.location.chunk)) return

		particleLocation.world = world
		particleLocation.x = soul.location.x
		particleLocation.y = soul.location.y + 1.0
		particleLocation.z = soul.location.z

		when (soul.state) {
			SoulState.NORMAL -> renderNormalState(world)
			SoulState.PANIC -> renderPanicState(world)
			SoulState.EXPLODING -> handleExplosion(world)
		}
	}

	private fun renderNormalState(world: World) {
		particleLocation.y = soul.location.y + 1.0 + sin(System.currentTimeMillis() * 0.001) * 0.5
		world.spawnParticle(Particle.END_ROD, particleLocation, 5, 0.01, 0.01, 0.01, 0.01, null, true)

		particleLocation.y = soul.location.y + 1.0
		world.spawnParticle(Particle.SCULK_SOUL, particleLocation, 1, 0.3, 0.5, 0.3, 0.01, null, true)

		// Shine green if soul only contains XP
		val shineParticle = if (soul.isOnlyXP) Particle.COMPOSTER else Particle.ELECTRIC_SPARK
		world.spawnParticle(shineParticle, particleLocation, 1, 0.8, 0.5, 0.8, 0.0, null, true)
	}

	private fun renderPanicState(world: World) {
		if (soul.timeLeft > 1) {
			val sculkParticleCount = min(soul.timeLeft / 6, 5)
			world.spawnParticle(Particle.SCULK_SOUL, particleLocation, sculkParticleCount, 0.01, 0.01, 0.01, 0.1, null, true)
			world.spawnParticle(Particle.END_ROD, particleLocation, 1, 0.01, 0.01, 0.01, 0.01, null, true)
		}
	}

	private fun handleExplosion(world: World) {
		if (!soul.implosion) {
			soul.implosion = true

			val x = particleLocation.x
			val y = particleLocation.y
			val z = particleLocation.z

			object : BukkitRunnable() {
				override fun run() {
					world.spawnParticle(Particle.SONIC_BOOM, Location(world, x, y, z), 1, 0.0, 0.0, 0.0, 0.0, null, true)
					cancel()
				}
			}.runTaskLater(SoulGraves.plugin, 8)
		}
	}
}