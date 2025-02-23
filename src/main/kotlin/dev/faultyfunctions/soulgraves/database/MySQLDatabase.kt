package dev.faultyfunctions.soulgraves.database

import com.saicone.rtag.item.ItemTagStream
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.DatabaseManager
import dev.faultyfunctions.soulgraves.managers.SERVER_NAME
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.Location
import java.sql.SQLException
import java.util.*

class MySQLDatabase private constructor() {

    // DATABASE VALUES
    private var dataSource: HikariDataSource
    private var jdbcUrl: String
    private var jdbcDriver: String
    private var username: String
    private var password: String
    private val databaseName: String = "soul_grave"

    init {
        val config = DatabaseManager.databaseConfig

        jdbcUrl = config.getString("MySQL.jdbc-url")!!
        jdbcDriver = config.getString("MySQL.jdbc-class")!!
        username = config.getString("MySQL.properties.user")!!
        password = config.getString("MySQL.properties.password")!!

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        hikariConfig.driverClassName = jdbcDriver
        hikariConfig.username = username
        hikariConfig.password = password
        dataSource = HikariDataSource(hikariConfig)
    }

    companion object {
        val instance: MySQLDatabase by lazy { MySQLDatabase().apply {
            createTable()
            initCurrentServerSouls()
            SoulGraves.plugin.logger.info("Connected to MySQL Database!")
        } }
    }

    // Create Table
    private fun createTable() {
        dataSource.connection.use { connection ->
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS $databaseName (
                markerUUID VARCHAR(255) PRIMARY KEY,
                ownerUUID VARCHAR(255),
                serverName VARCHAR(255),
                x INT,
                y INT,
                z INT,
                inventory TEXT,
                xp INT,
                deathTime BIGINT,
                expireTime BIGINT,
                freezeTime BIGINT,
                isDeleted BIT(1) DEFAULT 0)
                """.trimIndent()
            ).use { statement ->
                try {
                    statement.executeUpdate()
                    println("Table '$databaseName' created successfully.")
                } catch (e: SQLException) {
                    println("Error while creating table: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    // Read Souls in Current Server
    private fun initCurrentServerSouls() {
        val souls = mutableListOf<Soul>()
        // CURRENT SERVER SOULS
        souls += SoulGraves.soulList

        dataSource.connection.use{ connection ->
            connection.prepareStatement("""
                SELECT * FROM $databaseName WHERE serverName = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, SERVER_NAME)
                try {
                    val resultSet = statement.executeQuery()
                    while (resultSet.next()) {
                        // TODO freezeTime?
                        if (resultSet.getLong("expireTime") <= System.currentTimeMillis()) continue

                        val soul = Soul.initAndStart(
                            markerUUID = UUID.fromString(resultSet.getString("markerUUID")),
                            ownerUUID = UUID.fromString(resultSet.getString("ownerUUID")),
                            inventory = ItemTagStream.INSTANCE.listFromBase64(resultSet.getString("inventory")),
                            xp = resultSet.getInt("xp"),
                            location = Location(
                                Bukkit.getWorld(resultSet.getString("world")),
                                resultSet.getDouble("x"),
                                resultSet.getDouble("y"),
                                resultSet.getDouble("z")
                            ),
                            deathTime = resultSet.getLong("deathTime"),
                            expireTime = resultSet.getLong("expireTime"),
                            freezeTime = resultSet.getLong("freezeTime")
                        )
                        // IF FOUND DELETED TAG, SOUL WILL REMOVE.
                        if (resultSet.getBoolean("isDeleted")) { soul.delete() }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun saveSoul(soul: Soul) {
        val now = System.currentTimeMillis()
        dataSource.connection.use { connection ->
            connection.prepareStatement("""
            INSERT INTO $databaseName 
            (markerUUID, ownerUUID, serverName, world, x, y, z, inventory, xp, deathTime, expireTime, freezeTime, isDeleted) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            ownerUUID = VALUES(ownerUUID),
            serverName = VALUES(serverName),
            world = VALUES(world),
            x = VALUES(x),
            y = VALUES(y),
            z = VALUES(z),
            inventory = VALUES(inventory),
            xp = VALUES(xp),
            deathTime = VALUES(deathTime),
            expireTime = VALUES(expireTime),
            freezeTime = VALUES(freezeTime),
            isDeleted = VALUES(isDeleted)
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, soul.markerUUID.toString())
                statement.setString(2, soul.ownerUUID.toString())
                statement.setString(3, soul.serverId)
                statement.setString(4, soul.location.world?.name)
                statement.setInt(5, soul.location.x.toInt())
                statement.setInt(6, soul.location.y.toInt())
                statement.setInt(7, soul.location.z.toInt())
                statement.setString(8, ItemTagStream.INSTANCE.listToBase64(soul.inventory))
                statement.setInt(9, soul.xp)
                statement.setLong(10, soul.deathTime)
                statement.setLong(11, (ConfigManager.timeStable + ConfigManager.timeUnstable) * 1000L + now)
                statement.setLong(12, soul.freezeTime)
                statement.setBoolean(13, false)
                try {
                    statement.executeUpdate()
                } catch (e: SQLException) {
                    SoulGraves.plugin.logger.severe("Failed to save soul")
                    e.printStackTrace()
                }
            }
        }
    }


    // Save Soul Copy
    fun saveSoulCopy(soul: Soul) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("""
            UPDATE $databaseName SET 
            ownerUUID = ? ,
            inventory = ? ,
            xp = ? ,
            expireTime = ? ,
            freezeTime = ?
            WHERE markerUUID = ?
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, soul.ownerUUID.toString())
                statement.setString(2, ItemTagStream.INSTANCE.listToBase64(soul.inventory))
                statement.setInt(3, soul.xp)
                statement.setLong(4, soul.expireTime)
                statement.setLong(5, soul.freezeTime)
                try {
                    statement.executeUpdate()
                } catch (e: SQLException) {
                    SoulGraves.plugin.logger.severe("Failed to save soul copy")
                    e.printStackTrace()
                }
            }
        }
    }


    // Read Souls in Current Server
    fun getAllSouls() : MutableList<Soul> {
        val souls = mutableListOf<Soul>()
        // CURRENT SERVER SOULS
        souls += SoulGraves.soulList

        dataSource.connection.use{ connection ->
            connection.prepareStatement("""
                SELECT * FROM $databaseName
                AND serverName != ?
                AND isDeleted = FALSE
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, SERVER_NAME)
                try {
                    val resultSet = statement.executeQuery()
                    while (resultSet.next()) {
                        // TODO freezeTime?
                        if (resultSet.getLong("expireTime") <= System.currentTimeMillis()) continue

                        souls.add(Soul.createDataCopy(
                            markerUUID = UUID.fromString(resultSet.getString("markerUUID")),
                            ownerUUID = UUID.fromString(resultSet.getString("ownerUUID")),
                            inventory = ItemTagStream.INSTANCE.listFromBase64(resultSet.getString("inventory")),
                            xp = resultSet.getInt("xp"),
                            location = Location(Bukkit.getWorld(resultSet.getString("world")), resultSet.getDouble("x"), resultSet.getDouble("y"), resultSet.getDouble("z")),
                            serverId = resultSet.getString("serverName"),
                            deathTime = resultSet.getLong("deathTime"),
                            expireTime = resultSet.getLong("expireTime"),
                            freezeTime = resultSet.getLong("freezeTime")
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return souls
    }


    // Read Souls From Database
    fun getPlayerSouls(playerUUID: UUID) : MutableList<Soul> {
        val souls = mutableListOf<Soul>()
        // CURRENT SERVER SOULS
        souls += SoulGraves.soulList.filter { it.ownerUUID == playerUUID }

        dataSource.connection.use{ connection ->
            connection.prepareStatement("""
                SELECT * FROM $databaseName
                WHERE ownerUUID = ?
                AND serverName != ?
                AND isDeleted = FALSE
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, playerUUID.toString())
                statement.setString(2, SERVER_NAME)
                try {
                    val resultSet = statement.executeQuery()
                    while (resultSet.next()) {
                        // TODO freezeTime?
                        if (resultSet.getLong("expireTime") <= System.currentTimeMillis()) continue

                        souls.add(Soul.createDataCopy(
                            markerUUID = UUID.fromString(resultSet.getString("markerUUID")),
                            ownerUUID = UUID.fromString(resultSet.getString("ownerUUID")),
                            inventory = ItemTagStream.INSTANCE.listFromBase64(resultSet.getString("inventory")),
                            xp = resultSet.getInt("xp"),
                            location = Location(Bukkit.getWorld(resultSet.getString("world")), resultSet.getDouble("x"), resultSet.getDouble("y"), resultSet.getDouble("z")),
                            serverId = resultSet.getString("serverName"),
                            deathTime = resultSet.getLong("deathTime"),
                            expireTime = resultSet.getLong("expireTime"),
                            freezeTime = resultSet.getLong("freezeTime")
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return souls
    }


    // Read Soul From Database
    fun getSoul(markerUUID: UUID) : Soul? {
        // IF IN CURRENT SERVER SOULS
        val currentServerSouls = SoulGraves.soulList.stream().filter { it.markerUUID == markerUUID }.toList()
        if (currentServerSouls.isNotEmpty()) return currentServerSouls[0]

        dataSource.connection.use{ connection ->
            connection.prepareStatement("""
                SELECT * FROM $databaseName
                WHERE markerUUID = ?
                AND isDeleted = FALSE
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, markerUUID.toString())
                try {
                    val resultSet = statement.executeQuery()
                    // TODO freezeTime?
                    if (resultSet.getLong("expireTime") <= System.currentTimeMillis()) return null

                    return Soul.createDataCopy(
                        markerUUID = UUID.fromString(resultSet.getString("markerUUID")),
                        ownerUUID = UUID.fromString(resultSet.getString("ownerUUID")),
                        inventory = ItemTagStream.INSTANCE.listFromBase64(resultSet.getString("inventory")),
                        xp = resultSet.getInt("xp"),
                        location = Location(Bukkit.getWorld(resultSet.getString("world")), resultSet.getDouble("x"), resultSet.getDouble("y"), resultSet.getDouble("z")),
                        serverId = resultSet.getString("serverName"),
                        deathTime = resultSet.getLong("deathTime"),
                        expireTime = resultSet.getLong("expireTime"),
                        freezeTime = resultSet.getLong("freezeTime")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }


    // Mark a Soul Deleted
    fun markSoulDelete(makerUUID: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("""
                UPDATE $databaseName SET isDeleted = 1 WHERE markerUUID = ?
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, makerUUID.toString())
                try {
                    statement.executeUpdate()
                } catch (e: SQLException) {
                    println("Error while mark soul delete: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }


    // Mark a Soul Explode
    fun markSoulExplode(makerUUID: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("""
                UPDATE $databaseName SET expireTime = 1 WHERE markerUUID = ?
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, makerUUID.toString())
                try {
                    statement.executeUpdate()
                } catch (e: SQLException) {
                    println("Error while mark soul explode: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }


    // Delete Soul From Database
    fun deleteSoul(soul: Soul) {
        val uuid = soul.markerUUID
        deleteSoul(uuid)
    }
    fun deleteSoul(markerUUID: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("""
                DELETE FROM $databaseName WHERE markerUUID = ?
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, markerUUID.toString())
                try {
                    statement.executeUpdate()
                } catch (e: SQLException) {
                    println("Error while delete soul: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

}