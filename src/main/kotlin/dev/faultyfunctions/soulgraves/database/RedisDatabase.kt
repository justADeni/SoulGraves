package dev.faultyfunctions.soulgraves.database

import com.google.gson.JsonSyntaxException
import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.SoulGraveAPI
import dev.faultyfunctions.soulgraves.database.MessageAction.*
import dev.faultyfunctions.soulgraves.managers.DatabaseManager
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RedisDatabase {

    private var redisClient: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null
    private var callbackExecutor: ExecutorService? = null
    private val pluginChannel: String = "soulgraves:main"


    init {
        val config = DatabaseManager.databaseConfig
        val uri = config.getString("Redis.uri")

        try {
            // Create Redis Client
            redisClient = RedisClient.create(uri)
            connection = redisClient!!.connect()
            pubSubConnection = redisClient!!.connectPubSub()
            callbackExecutor = Executors.newWorkStealingPool(6)

            // Auto Sub Channel
            pubSubConnection!!.async().subscribe(pluginChannel)

            // Create Listener
            pubSubConnection!!.addListener(object : RedisPubSubAdapter<String, String>() {
                override fun message(channel: String, message: String) {
                    println("Received message: $message from channel: $channel")
                    if (pluginChannel != channel) return
                    handleMessage(message)
                }
            })

            SoulGraves.plugin.logger.info("Redis Database Connect Successed!")
        } catch (e: Exception) {
            SoulGraves.plugin.logger.info("Redis Database Can Not Connect!")
            e.printStackTrace()
            SoulGraves.plugin.server.pluginManager.disablePlugin(SoulGraves.plugin)
        }
    }

    companion object {
        val instance: RedisDatabase by lazy { RedisDatabase() }
        @JvmStatic
        fun getInstanceByJava(): RedisDatabase {
            return instance
        }
    }


    /**
     * Publish Plugin Message to Redis
     * @param packet data
     */
    fun publish(packet: RedisPacket) {
        val message = packet.toJson()
        println(message)
        val async = connection!!.async()
        async.publish(pluginChannel, message)
    }


    /**
     * Handle Message
     * @param message data
     */
    fun handleMessage(message: String?) {

        // EMPTY MESSAGE
        if (message == null) {
            SoulGraves.plugin.logger.warning("Received null message from Redis")
            return
        }

        try {
            val packet = RedisPacket.fromJson(message)
            when (packet.action) {
                // REMOVE_SOUL
                // PAYLOAD FORMAT: [MAKER_UUID]
                REMOVE_SOUL -> {
                    callbackExecutor!!.execute {
                        val makerUUID = packet.payload
                        val soul = SoulGraveAPI.getSoul(UUID.fromString(makerUUID))
                        soul?.let { Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable { soul.delete() }) } // Sync for Bukkit API
                    }
                }
                // EXPLODE_SOUL
                // PAYLOAD FORMAT: [MAKER_UUID]
                EXPLODE_SOUL -> {
                    callbackExecutor!!.execute {
                        val makerUUID = packet.payload
                        val soul = SoulGraveAPI.getSoul(UUID.fromString(makerUUID))
                        soul?.let { Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable { soul.explodeNow() }) } // Sync for Bukkit API
                    }
                }
            }
        } catch (e: JsonSyntaxException) {
            SoulGraves.plugin.logger.severe("JSON PARSED FAILED: ${e.message}")
            SoulGraves.plugin.logger.severe("ORIGIN MESSAGE: ${message.take(200)}...")
        } catch (e: Exception) {
            SoulGraves.plugin.logger.severe("Unexpected Error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }


    /**
     * Disable Redis Connect.
     */
    fun shutdown() {
        callbackExecutor?.shutdown()
        pubSubConnection?.close()
        connection?.close()
        redisClient?.shutdown()
    }
}