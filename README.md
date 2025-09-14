
# 🪦Soul Graves🪦
[![Static Badge](https://img.shields.io/badge/license-MIT-plum)](https://github.com/FaultyFunctions/SoulGraves/blob/main/LICENSE.md)
[![Static Badge](https://img.shields.io/badge/jdk-21-plum)]()
[![Static Badge](https://img.shields.io/badge/paper-1.20.6%20--%201.21.x-skyblue)](https://papermc.org)
[![Static Badge](https://img.shields.io/badge/spigot-1.20.6%20--%201.21.x-d48c02)](https://spigotmc.org)
[![Static Badge](https://img.shields.io/badge/downloads-Modrinth-forestgreen)](https://modrinth.com/plugin/soul-graves)
[![Static Badge](https://img.shields.io/badge/downloads-Hangar-blue)](https://hangar.papermc.io/Faulty/SoulGraves)
[![Static Badge](https://img.shields.io/badge/downloads-Spigot-d48c02)](https://www.spigotmc.org/resources/soul-graves.121065)

A unique graves plugin where players collect their souls to retrieve their belongings when they die. A soul will spawn at your death location that provides audio and visual feedback to help you locate it. Once you find it you can retrieve your items by walking into your soul. Be careful though, wait too long and your soul will burst dropping all your items!

Special thanks to [Catnies](https://github.com/Catnies) and [ShiroKazane](https://github.com/ShiroKazane) for their contributions to the project!

## Features
### ⭐ Core Mechanics
- Soul Graves will spawn a soul when a player dies, storing their belongings.
- Avoids spawning souls in liquids, the void, or non-solid blocks.
- Souls persist through server restarts.
- Customizable number of active souls per player, with an option for unlimited.
- Ability to restrict soul collection to their owner.
- Config options to pause the soul countdown timer while the owner is offline.
- Disable soul spawning in specific worlds.

### ✨ Visual & Audio Feedback
- Visual cue indicates when a soul is about to burst.
- Players in a configurable radius will hear heartbeat sounds to guide the to nearby souls.
- Optional and customizable hint particles to lead players directly to their soul.
- Choose which sounds play for soul collection, soul bursting, and notifying players.
- Both the soul’s owner and nearby players can be notified when a soul's status changes.

### 💎 Item & XP Handling
- Souls can be configured to only drop items, xp, or both when they burst.
- Set distinct XP return percentages for owners and non-owners separately.
- Option to either destroy or scatter items/xp when a soul bursts.

### 🌐 Cross-Server Support
- Cross-server support with MySQL and Redis for a seamless experience.

### 🪙 Economy Support
- Configurable costs for players teleporting to their soul.

### 🛠️ Customization & Developer API
- All messages are fully customizable with MiniMessage formatting.
- Custom API for developers to hook into to customize Soul Graves further.

## Compatibility
- **WorldGuard**: Flag for disabling soul spawning in specific regions.
- **Towny**: Supporting their various keep-inventory options.
- **Soulbound Enchantment Plugins**:
	- Vane
	- ExcellentEnchants
	- EcoEnchants

## Showcase
#### A stable soul waiting to be collected:
https://github.com/user-attachments/assets/8de28a77-0b7b-4333-af3f-11baab370c6b

#### A unstable soul that bursts and drops its contents:
https://github.com/user-attachments/assets/beaa0011-35df-476a-94f8-8b06da72a618

## Commands & Permissions
- **/soulgraves** or **/sg**: The main command for Soul Graves. Will give version information to the sender.
	- Permission: `soulgraves.command`
- **/soulgraves reload**: Reload the plugin's configuration files.
	- Permission: `soulgraves.command.reload`
- **/soulgraves back**: Teleports the sender to their latest soul. Only works in-game.
	- Permission: `soulgraves.command.back`
#### Other Permissions
- `soulgraves.spawn`: Allows a soul to spawn if `permission-required` is true in the config.

## Configuration
<details>
<summary>config.yml</summary>

```yml
# DO NOT EDIT file-version DIRECTLY
file-version: 4

# If set true, players will require "soulgraves.spawn" permission to spawn a soul upon death
permission-required: false

# Time in seconds for how long a soul remains in its stable state before becoming unstable
time-stable: 240

# Time in seconds for how long a soul will show the unstable animation for before bursting
# The total time the soul is available to collect is time-stable + time-unstable
time-unstable: 60

# How much it costs to teleport to your soul, set to 0.0 to disable
teleport-cost: 0.0

# Whether to freeze the timer when the owner of the soul is offline
# This feature cannot detect the online status of players in other subservers on a proxy server
offline-owner-timer-freeze: false

# Whether to notify nearby players when a soul bursts
notify-nearby-players: true

# The radius in blocks to alert nearby players when a soul bursts
notify-radius: 128

# Whether to notify the owner of a soul when it is collected by another player
notify-owner-pickup: true

# The percentage of the soul's XP to give to the owner of the soul when it is collected by the owner
xp-percentage-owner: 0.5

# The percentage of the soul's XP to give to a player who isn't the owner when the soul is collected by that player
xp-percentage-others: 0.2

# The percentage of the soul's XP to drop when the soul bursts
xp-percentage-burst: 0.2

# Whether souls are only collectible by their owners
owner-locked: false

# The maximum number of souls a player can hold simultaneously. Set to 0 for unlimited.
# If the limit is exceeded, the oldest soul will explode
max-souls-per-player: 0

# Whether souls will drop items when they burst
souls-drop-items: true

# Whether souls will drop XP when they burst
souls-drop-xp: true

# Whether souls will store items when they are created
# If both souls-store-items and souls-store-xp are false, souls will not spawn
souls-store-items: true

# Whether souls will store XP when they are created
# If both souls-store-items and souls-store-xp are false, souls will not spawn
souls-store-xp: true

# What sounds to play when a soul is collected
# The format is 'soundEvent, volume, pitch'
# The soundKey can be found at https://minecraft.wiki/w/Sounds.json#Java_Edition_values under the 'Sound Event' column
pickup-sound:
  enabled: true
  sounds:
    - 'minecraft:block.amethyst_block.break, 1.0, 0.5'
    - 'minecraft:entity.player.levelup, 1.0, 2.0'
    - 'minecraft:block.amethyst_block.resonate, 1.0, 0.5'

# What sounds to play when a soul bursts
# The format is 'soundEvent, volume, pitch'
# The soundKey can be found at https://minecraft.wiki/w/Sounds.json#Java_Edition_values under the 'Sound Event' column
burst-sound:
  enabled: true
  sounds:
    - 'minecraft:block.glass.break, 3.0, 1.0'
    - 'minecraft:entity.vex.death, 3.0, 0.5'
    - 'minecraft:entity.allay.death, 3.0, 0.5'
    - 'minecraft:entity.warden.sonic_boom, 3.0, 0.5'

# What sounds to play to notify nearby players when a soul bursts
# The format is 'soundEvent, volume, pitch'
# The soundKey can be found at https://minecraft.wiki/w/Sounds.json#Java_Edition_values under the 'Sound Event' column
notify-nearby-sound:
  enabled: true
  sounds:
    - 'minecraft:block.amethyst_block.resonate, 1.0, 1.0'

# What sounds to play to the owner when their soul bursts
# The format is 'soundEvent, volume, pitch'
# The soundKey can be found at https://minecraft.wiki/w/Sounds.json#Java_Edition_values under the 'Sound Event' column
notify-owner-burst-sound:
  enabled: true
  sounds:
    - 'minecraft:block.amethyst_block.break, 1.0, 0.5'

# What sounds to play to the owner when their soul is collected by another player
# The format is 'soundEvent, volume, pitch'
# The soundKey can be found at https://minecraft.wiki/w/Sounds.json#Java_Edition_values under the 'Sound Event' column
notify-owner-pickup-sound:
  enabled: true
  sounds:
    - 'minecraft:block.beacon.deactivate, 1.0, 0.5'

# What worlds to disable spawning a soul in
# If none, leave a blank array
# Usage:
#disabled-worlds:
#  - world_nether
#  - world_the_end
disabled-worlds: []

# Controls particles that will lead the player to their soul
hint-particles:
  enabled: true
  activation-radius: 128 # The radius around the soul to show hint particles, set to 0 to always show hint particles
  tracked-soul: 'OLDEST' # Which soul should we track if the player has multiple? Options: OLDEST, NEWEST
  particle-type: 'END_ROD'
  start-distance: 5 # How far away from the player the particles should start
  mode: 'TRAIL' # Options: TRAIL, WANDER
  trail:
    length: 8 # How long the particle trail towards the soul should be
    density: 2 # How many particles to spawn per block distance
  wander:
    count: 5 # How many particles should be spawned
    min-speed: 0.2 # The minimum speed of the particles
    max-speed: 0.6 # The maximum speed of the particles
```

</details>

<details>
<summary>database.yml</summary>

```yml
# DO NOT EDIT file-version DIRECTLY
file-version: 1

# NOTE: If you change this config, you'll need to restart the server in order for the changes to take effect.

# Options: PDC, CROSS_SERVER
# If you use CROSS_SERVER, you must set configure both the MySQL and Redis sections
storage-mode: PDC

# Server name for cross-server storage
# Ensure that each server's name is unique when using CROSS_SERVER storage
# WARNING: Changing this value after initializing the database will cause data loss
server-name: "lobby"

# Database config for cross-server storage
# Set useSSL to true if your database supports it
MySQL:
  jdbc-url: "jdbc:mysql://localhost:3306/minecraft?useSSL=false&autoReconnect=true"
  jdbc-class: "com.mysql.cj.jdbc.Driver"
  properties:
    user: "username"
    password: "password"

Redis:
  uri: "redis://localhost:6379/0"
```

</details>

<details>
<summary>messages.yml</summary>

```yml
# DO NOT EDIT file-version DIRECTLY
file-version: 4

# Message to send the player when they run the reload commands
soul-graves-reload: "[<dark_aqua>Soul Graves</dark_aqua>] Config reloaded!"
# Message to send to the owner when their soul bursts
soul-burst: "<dark_aqua>☠ Your soul has burst!</dark_aqua>"
# Message to send to the owner when their soul bursts and souls-drop-items is true
soul-burst-drop-items: "<red>☀ Any belongings inside have been scattered!</red>"
# Message to send to the owner when their soul bursts and souls-drop-items is false
soul-burst-lose-items: "<red>✖ Any belongings inside have been destroyed!</red>"
# Message to send when a soul bursts nearby
soul-burst-nearby: "<dark_aqua>☠ A soul has burst nearby.</dark_aqua>"
# Message to send when a soul is collected
soul-collect: "<green>✦ You've collected the soul's contents!</green>"
# Message to send to the owner when another player has collected their soul
soul-collect-other: "<light_purple>⚑ Someone else has collected your soul!</light_purple>"
# Message to send when spawn a new soul would exceed the player's limit (oldest will explode)
soul-limit-explode: "<yellow>⚠ Reached soul limit (%max%), your oldest soul has burst!</yellow>"
# Message to send when a player tries to return to their soul but has no soul
command-back-no-soul: "<yellow>✖ You don't have a soul to teleport to.</yellow>"
# Message to send when a player doesn't have enough funds to return to their soul
command-back-no-funds: "<red>✖ You don't have enough funds to teleport to your soul.</red>"
# Message to send when a player successfully returns to their soul without a cost
command-back-success-free: "<gold>✔ You have teleported to your soul.</gold>"
# Message to send when a player successfully returns to their soul
command-back-success-paid: "<gold>✔ You have teleported to your soul for %cost% coins.</gold>"
```

</details>

## API

<details>
<summary>Maven</summary>

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
```xml
	<dependency>
	    <groupId>com.github.FaultyFunctions</groupId>
	    <artifactId>SoulGraves</artifactId>
	    <version>TAG</version>
	</dependency>
```
</details>
<details>
<summary>Gradle</summary>

```groovy
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```
```groovy
	dependencies {
	implementation 'com.github.FaultyFunctions:SoulGraves:TAG'
}
```

</details>

Replace `TAG` with the release tag on GitHub of your desired version.

## Third-Party Addons
- [SoulGravesPlus](https://github.com/JosTheDude/SoulGravesPlus) by [@JosTheDude](https://github.com/JosTheDude)
- [SoulGravesBanco](https://github.com/justADeni/SoulGravesBanco) by [@justADeni](https://github.com/justADeni)

## Roadmap
* [Make a suggestion!](https://github.com/FaultyFunctions/SoulGraves/issues)

## Acknowledgements
- [Vanilla Refresh](https://modrinth.com/datapack/vanilla-refresh) - Based on their idea of "Soul Links"
- [B's Ghost Graves](https://modrinth.com/plugin/bs-ghostgrave) - Similar plugin inspired by Hollow Knight. They shared their source so I could learn from it, huge thanks!
- [MorePersistentDataTypes](https://github.com/mfnalex/MorePersistentDataTypes) - Great PDC library
- [BoostedYAML](https://github.com/dejvokep/boosted-yaml) - YAML configuration library
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - For handling JDBC connections
- [Lettuce](https://github.com/redis/lettuce) - Sending/receiving Redis messages
- [Lamp](https://github.com/Revxrsal/Lamp) - Minecraft command library
- [rtag](https://github.com/saicone/rtag) - Reading/writing NBT data
- [adventure](https://github.com/KyoriPowered/adventure) - UI library for Minecraft
- [bStats](https://github.com/Bastian/bStats) - Plugin tracking metrics
