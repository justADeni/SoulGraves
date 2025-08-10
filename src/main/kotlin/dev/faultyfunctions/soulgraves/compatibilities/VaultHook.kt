package dev.faultyfunctions.soulgraves.compatibilities

import dev.faultyfunctions.soulgraves.SoulGraves
import org.bukkit.entity.Player
import java.math.BigDecimal

class VaultHook {
	companion object {
		val instance: VaultHook by lazy { VaultHook() }
	}

	private var economy: Any? = null
	private var isVaultUnlocked: Boolean = false

	fun init() {
		// Try VaultUnlocked first using full reflection
		try {
			val vaultUnlockedClass = Class.forName("net.milkbowl.vault2.economy.Economy")
			val rsp = SoulGraves.plugin.server.servicesManager.getRegistration(vaultUnlockedClass)

			if (rsp != null) {
				economy = rsp.provider
				isVaultUnlocked = true
				SoulGraves.plugin.logger.info("[√] VaultUnlocked hook loaded!")
				return
			}
		} catch (_: ClassNotFoundException) {
			SoulGraves.plugin.logger.info("[!] VaultUnlocked not detected, trying regular Vault...")
		} catch (ex: Exception) {
			SoulGraves.plugin.logger.warning("[X] Error trying to hook into VaultUnlocked: ${ex.message}")
		}

		// Fall back to regular Vault
		try {
			val vaultClass = Class.forName("net.milkbowl.vault.economy.Economy")
			val rsp = SoulGraves.plugin.server.servicesManager.getRegistration(vaultClass)

			if (rsp != null) {
				economy = rsp.provider
				isVaultUnlocked = false
				SoulGraves.plugin.logger.info("[√] Vault hook loaded!")
				return
			}
		} catch (_: ClassNotFoundException) {
			SoulGraves.plugin.logger.warning("[X] Vault class not found")
		} catch (ex: Exception) {
			SoulGraves.plugin.logger.warning("[X] Error trying to hook into Vault: ${ex.message}")
		}

		SoulGraves.plugin.logger.warning("[X] No economy provider found! Neither VaultUnlocked nor Vault could be hooked.")
	}

	fun withdraw(player: Player, amount: BigDecimal): Boolean {
		if (economy == null) return false

		return if (isVaultUnlocked) {
			// Cast safely at runtime
			val vaultUnlockedEcon = economy!!
			try {
				val withdrawMethod = vaultUnlockedEcon.javaClass.getMethod("withdraw", String::class.java, java.util.UUID::class.java, BigDecimal::class.java)
				val result = withdrawMethod.invoke(vaultUnlockedEcon, SoulGraves.plugin.name, player.uniqueId, amount)
				val transactionSuccessMethod = result.javaClass.getMethod("transactionSuccess")
				transactionSuccessMethod.invoke(result) as Boolean
			} catch (ex: Exception) {
				SoulGraves.plugin.logger.warning("[X] Error using VaultUnlocked economy: ${ex.message}")
				false
			}
		} else {
			// Regular Vault can use direct casting because it's included in your plugin's dependencies
			(economy as? net.milkbowl.vault.economy.Economy)?.withdrawPlayer(player, amount.toDouble())?.transactionSuccess() ?: false
		}
	}

	fun has(player: Player, amount: BigDecimal): Boolean {
		if (economy == null) return false

		return if (isVaultUnlocked) {
			val vaultUnlockedEcon = economy!!
			try {
				val hasMethod = vaultUnlockedEcon.javaClass.getMethod("has", String::class.java, java.util.UUID::class.java, BigDecimal::class.java)
				hasMethod.invoke(vaultUnlockedEcon, SoulGraves.plugin.name, player.uniqueId, amount) as Boolean
			} catch (ex: Exception) {
				SoulGraves.plugin.logger.warning("[X] Error using VaultUnlocked economy: ${ex.message}")
				false
			}
		} else {
			(economy as? net.milkbowl.vault.economy.Economy)?.has(player, amount.toDouble()) ?: false
		}
	}
}