package dev.faultyfunctions.soulgraves.database

import com.google.gson.JsonSyntaxException
import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.api.RedisPublishAPI.pendingAnswersRequests
import dev.faultyfunctions.soulgraves.api.SoulGraveAPI
import dev.faultyfunctions.soulgraves.database.MessageAction.*
import dev.faultyfunctions.soulgraves.managers.*
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.*

class RedisDatabase private constructor() {

    private var redisClient: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null
    private var callbackExecutor: ExecutorService? = null
    private val pluginChannel: String = "soulgraves:main"

    private val heartbeatInterval = 5L // Heartbeat Interval (Seconds)
    private val heartbeatTimeout = 15L // TimeOut Interval (Seconds)
    private val serverStatus = ConcurrentHashMap<String, Long>()

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
                    if (pluginChannel != channel) return
                    handleMessage(message)
                }
            })

            // Server Heartbeat
            startHeartbeatTask()
            startCleanupTask()

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
     * Server Heartbeats
     */
    // Timed publish to Redis current server is online
    private fun startHeartbeatTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(SoulGraves.plugin, Runnable {
            val timestamp = System.currentTimeMillis()
            val message = "$SERVER_NAME|$timestamp"
            publish(RedisPacket(SERVER_NAME, HEARTBEAT, message))
        }, 0L, heartbeatInterval * 20) // Convert seconds to ticks
    }
    // Timed Clean Expire Server Status Cache
    private fun startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(SoulGraves.plugin, Runnable {
            val currentTime = System.currentTimeMillis()
            serverStatus.entries.removeIf { (_, lastSeen) ->
                currentTime - lastSeen > TimeUnit.SECONDS.toMillis(heartbeatTimeout)
            }
        }, 0L, heartbeatTimeout * 10L * 20) // Cleanup interval
    }
    // Shutdown Heartbeat at Server Stop
    private fun shutdownHeartbeat() {
        publish(RedisPacket(SERVER_NAME, HEARTBEAT_SHUTDOWN, SERVER_NAME))
    }

    fun isServerOnline(server: String): Boolean {
        val lastSeen = serverStatus[server] ?: return false
        return System.currentTimeMillis() - lastSeen < TimeUnit.SECONDS.toMillis(heartbeatTimeout)
    }
    fun getOnlineServers(): List<String> {
        val currentTime = System.currentTimeMillis()
        return serverStatus.keys.filter { server ->
            currentTime - (serverStatus[server] ?: 0) < TimeUnit.SECONDS.toMillis(heartbeatTimeout)
        }
    }



    /**
     * Publish Plugin Message to Redis
     * @param packet data
     */
    fun publish(packet: RedisPacket) {
        val message = packet.toJson()
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
            val sender = packet.senderId
            when (packet.action) {
                // REMOVE_SOUL
                // PAYLOAD FORMAT: [MSG_UUID][TARGET_SERVER][MAKER_UUID]
                REMOVE_SOUL -> {
                    callbackExecutor!!.execute {
                        val (msgUUID, targetServer, makerUUID) = packet.payload.split("|", limit = 3)
                        if (targetServer != SERVER_NAME) return@execute

                        // if soul list contain Soul
                        Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
                            val soul = SoulGraveAPI.getSoul(UUID.fromString(makerUUID))
                            if (soul != null) {
                                soul.delete()
                                publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|true"))
                            } else {
                                publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|false"))
                            }
                        }) // Sync for Bukkit API
                    }

                }

                // EXPLODE_SOUL
                // PAYLOAD FORMAT: [MSG_UUID][TARGET_SERVER][MAKER_UUID]
                EXPLODE_SOUL -> {
                    callbackExecutor!!.execute execute@ {
                        val (msgUUID, targetServer, makerUUID) = packet.payload.split("|", limit = 3)
                        if (targetServer != SERVER_NAME) return@execute

                        // if soul list contain Soul
                        Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
                            val soul = SoulGraveAPI.getSoul(UUID.fromString(makerUUID))
                            if (soul != null) {
                                soul.explode()
                                publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|true"))
                            } else {
                                publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|false"))
                            }
                        }) // Sync for Bukkit API
                    }
                }

                // UPDATE_SOUL
                // PAYLOAD FORMAT: [MSG_UUID][TARGET_SERVER][MAKER_UUID]
                UPDATE_SOUL -> {
                    callbackExecutor!!.execute {
                        val (msgUUID, targetServer, makerUUID) = packet.payload.split("|", limit = 3)
                        if (targetServer != SERVER_NAME) return@execute

                        val remoteCopySoul = MySQLDatabase.instance.getSoul(UUID.fromString(makerUUID))
                        if (remoteCopySoul != null) {
                            Bukkit.getScheduler().runTask(SoulGraves.plugin, Runnable {
                                val currentServerSoul = SoulGraveAPI.getSoul(UUID.fromString(makerUUID))
                                if (currentServerSoul != null) {
                                        currentServerSoul.ownerUUID = remoteCopySoul.ownerUUID
                                        currentServerSoul.inventory = remoteCopySoul.inventory
                                        currentServerSoul.xp = remoteCopySoul.xp

                                        currentServerSoul.expireTime = remoteCopySoul.expireTime
                                        currentServerSoul.timeLeft = (remoteCopySoul.expireTime - System.currentTimeMillis() / 1000).toInt()
                                        currentServerSoul.freezeTime = remoteCopySoul.freezeTime
                                        publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|true"))
                                } else {
                                    publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|false"))
                                }
                            })
                        } else {
                            publish(RedisPacket(SERVER_NAME, API_ANSWER, "$msgUUID|$sender|false"))
                        }
                    }
                }

                // NOTIFY_SOUL_EXPLODE
                // PAYLOAD FORMAT: [OWNER_UUID]
                NOTIFY_SOUL_EXPLODE -> {
                    callbackExecutor!!.execute {
                        val ownerUUID = UUID.fromString(packet.payload)
                        val player = Bukkit.getPlayer(ownerUUID)
                        if (player?.isOnline == true) {
                            if (MessageManager.soulBurstComponent != null)
                                SoulGraves.plugin.adventure().player(player).sendMessage(MessageManager.soulBurstComponent!!)
                            if (ConfigManager.soulsDropItems && MessageManager.soulBurstDropItemsComponent != null)
                                SoulGraves.plugin.adventure().player(player).sendMessage(MessageManager.soulBurstDropItemsComponent!!)
                            else if (MessageManager.soulBurstLoseItemsComponent != null)
                                SoulGraves.plugin.adventure().player(player).sendMessage(MessageManager.soulBurstLoseItemsComponent!!)
                        }
                    }
                }

                // NOTIFY_SOUL_OTHER_PICKUP
                // PAYLOAD FORMAT: [OWNER_UUID]
                NOTIFY_SOUL_OTHER_PICKUP -> {
                    callbackExecutor!!.execute {
                        val ownerUUID = UUID.fromString(packet.payload)
                        val player = Bukkit.getPlayer(ownerUUID)
                        if (player?.isOnline == true) {
                            if (MessageManager.soulCollectOtherComponent != null) SoulGraves.plugin.adventure().player(player).sendMessage(MessageManager.soulCollectOtherComponent!!)
                            if (ConfigManager.notifyOwnerPickupSound.enabled) {
                                ConfigManager.notifyOwnerPickupSound.sounds.forEachIndexed { index, soundKey ->
                                    player.playSound(player.location, soundKey, ConfigManager.notifyOwnerPickupSound.volumes[index], ConfigManager.notifyOwnerPickupSound.pitches[index])
                                }
                            }
                        }
                    }
                }

                // API_ANSWER
                // PAYLOAD FORMAT: [MSG_UUID][TARGET_SERVER][BOOLEAN]
                API_ANSWER -> {
                    callbackExecutor!!.execute {
                        val (msgUUID, targetServer, answer) = packet.payload.split("|", limit = 3)
                        if (targetServer != SERVER_NAME) return@execute

                        pendingAnswersRequests[msgUUID]?.complete(answer.toBoolean())
                    }
                }

                // HEARTBEAT
                // PAYLOAD FORMAT: [SERVER_NAME][TIMESTAMP]
                HEARTBEAT -> {
                    callbackExecutor!!.execute {
                        val (server, timestampStr) = packet.payload.split("|", limit = 2)
                        val timestamp = timestampStr.toLong()
                        serverStatus[server] = timestamp
                    }
                }

                // HEARTBEAT_SHUTDOWN
                // PAYLOAD FORMAT: [SERVER_NAME]
                HEARTBEAT_SHUTDOWN -> {
                    callbackExecutor!!.execute {
                        val server = packet.payload
                        serverStatus.remove(server)
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
        shutdownHeartbeat() // Stop Heartbeat
        callbackExecutor?.shutdown()
        pubSubConnection?.close()
        connection?.close()
        redisClient?.shutdown()
    }
}