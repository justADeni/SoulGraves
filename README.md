
# 🪦Soul Graves🪦
[![Static Badge](https://img.shields.io/badge/release-1.1.0-bisque)]()
[![Static Badge](https://img.shields.io/badge/license-MIT-plum)](https://github.com/FaultyFunctions/SoulGraves/blob/main/LICENSE.md)
[![Static Badge](https://img.shields.io/badge/paper-1.20.6%20--%201.21.x-skyblue)](https://papermc.org)
[![Static Badge](https://img.shields.io/badge/purpur-1.20.6%20--%201.21.x-e533ff)](https://purpurmc.org)
[![Static Badge](https://img.shields.io/badge/spigot-1.20.6%20--%201.21.x-d48c02)](https://spigotmc.org)
[![Static Badge](https://img.shields.io/badge/jdk-21-plum)]()
[![Static Badge](https://img.shields.io/badge/downloads-Modrinth-forestgreen)](https://modrinth.com/plugin/soul-graves)
[![Static Badge](https://img.shields.io/badge/downloads-Hangar-blue)](https://hangar.papermc.io/Faulty/SoulGraves)
[![Static Badge](https://img.shields.io/badge/downloads-Spigot-d48c02)](https://www.spigotmc.org/resources/soul-graves.121065)

A unique graves plugin where players collect their souls to retrieve their belongings when they die. A soul will spawn at your death location that provides audio and visual feedback to help you locate it. Once you find it you can retrieve your items by walking into your soul. Be careful though, wait too long and your soul will burst dropping all your items!

## Features
- Additional Minecraft death mechanics
- Fun particle effects
- Soul persists through restarts
- Visual cue to indicate when the soul is about to burst
- Audible heartbeat to make finding your soul easier
- Soul Graves will avoid spawning a soul in liquids, the void, and non-solid blocks
- Players can have multiple souls possible at the same time
- Option to only let owners retrieve their souls
- Option to make it so souls destroy items or XP when they burst
- Customizable XP return percentages
- Customizable messages
- Minimessage support

## GIFs
A stable soul waiting to be collected:

![javaw_YOdJmrwlg5](https://github.com/user-attachments/assets/0131abd3-e1da-4db4-ae97-826624ccee8f)

A destabilizing soul that bursts and drops its contents:

![Animation](https://github.com/user-attachments/assets/8ddf0d00-c7b7-4504-8fff-234f4f7af3dc)

## Configuration
<details>
<summary>config.yml</summary>

```yml
# Time in seconds for how long a soul remains in its stable state before becoming unstable
time-stable: 240

# Time in seconds for how long a soul will show the unstable animation for before bursting
# The total time the soul is available to collect is time-stable + time-unstable
time-unstable: 60

# Whether to notify nearby players when a soul bursts
notify-nearby-players: true

# The radius in blocks to alert nearby players when a soul bursts
notify-radius: 128

# The percentage of the soul's XP to give to the owner of the soul when it is collected by the owner
xp-percentage-owner: 0.5

# The percentage of the soul's XP to give to a player who isn't the owner when the soul is collected by that player
xp-percentage-others: 0.2

# The percentage of the soul's XP to drop when the soul bursts
xp-percentage-burst: 0.2

# Whether souls are only collectible by their owners
owner-locked: false

# Whether souls will drop items when they burst
souls-drop-items: true

# Whether souls will drop XP when they burst
souls-drop-xp: true

# What worlds to disable spawning a soul in
# If none, leave a blank array
# Usage:
#disabled-worlds:
#  - world_nether
#  - world_the_end
disabled-worlds: []
```

</details>

<details>
<summary>messages.yml</summary>

```yml
# Message to send to the owner when their soul bursts
soul-burst: "<dark_aqua>☠ Your soul has burst!"
# Message to send to the owner when their soul bursts and souls-drop-items is true
soul-burst-drop-items: "<red>☀ Any belongings inside have been scattered!"
# Message to send to the owner when their soul bursts and souls-drop-items is false
soul-burst-lose-items: "<red>✖ Any belongings inside have been destroyed!"
# Message to send when a soul bursts nearby
soul-burst-nearby: "<dark_aqua>☠ A soul has burst nearby!"
# Message to send when a soul is collected
soul-collect: "<green>✦ You've collected the soul's contents!"
# Message to send to the owner when another player has collected their soul
soul-collect-other: "<light_purple>⚑ Someone else has collected your soul!"
```

</details>

## Roadmap
* [Make a suggestion!](https://github.com/FaultyFunctions/SoulGraves/issues)

## Acknowledgements
- [Vanilla Refresh](https://modrinth.com/datapack/vanilla-refresh) - Based on their idea of "Soul Links"
- [B's Ghost Graves](https://modrinth.com/plugin/bs-ghostgrave) - Similar plugin inspired by Hollow Knight. They shared their source so I could learn from it, huge thanks!
- [MorePersistentDataTypes](https://github.com/mfnalex/MorePersistentDataTypes) - Great PDC library
