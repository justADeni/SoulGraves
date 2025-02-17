package dev.faultyfunctions.soulgraves.managers

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import dev.faultyfunctions.soulgraves.SoulGraves
import org.bukkit.Bukkit
import java.io.File
import java.io.IOException
import kotlin.properties.Delegates

object CrossServerManager {

    private lateinit var crossConfig: YamlDocument

    // CONFIG VALUES
    var serverId by Delegates.notNull<String>()



    fun loadConfig() {
        try {
            crossConfig = YamlDocument.create(
                File(SoulGraves.plugin.dataFolder, "cross-server.yml"),
                SoulGraves.plugin.getResource("cross-server.yml")!!,
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).setOptionSorting(
                    UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            )
            crossConfig.update()
            crossConfig.save()
        } catch (e: IOException) {
            SoulGraves.plugin.logger.severe("Failed to load cross-server.yml! The plugin will now shut down.")
            Bukkit.getServer().pluginManager.disablePlugin(SoulGraves.plugin)
        }

        // LOAD VALUES
        serverId = crossConfig.getString("name")
    }

}