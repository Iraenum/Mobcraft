package org.bench245.mobcraft.data

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PunishmentManager(private val plugin: Mobcraft) : CommandExecutor {

    private val punished = plugin.config.getConfigurationSection("punishedPlayers") ?: plugin.config.createSection("punishedPlayers")

    fun punish(player: Player) {
        val endTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000)

        punished.set(player.name, endTime)
        plugin.saveConfig()

        player.sendMessage("§cYou Have To Wait 24 Hours To Respawn!")
        player.gameMode = GameMode.SPECTATOR
    }

    fun unpunish(player: Player) {
        punished.set(player.name, null)
        plugin.saveConfig()
        player.gameMode = GameMode.SURVIVAL
        player.sendMessage("§aYou have been revived!")
    }

    fun isPunished(player: Player): Boolean {
        val end = punished.getLong(player.name)
        return end > System.currentTimeMillis()
    }
    fun getAllPunishedPlayers(): List<String> {
        return punished.getKeys(false).filter { punished.getLong(it) > System.currentTimeMillis() }
    }

    fun checkTimers() {
        for (name in punished.getKeys(false)) {
            val end = punished.getLong(name)
            if (end <= System.currentTimeMillis()) {
                val p = Bukkit.getPlayer(name)
                if (p != null) {
                    unpunish(p)
                } else {
                    punished.set(name, null)
                }
            }
        }
        plugin.saveConfig()
    }

    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): Boolean {
        TODO("Not yet implemented")
    }
}
