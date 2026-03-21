package org.bench245.mobcraft.data

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent

class PunishmentManager(val plugin: Mobcraft) : Listener {

    private val punished =
        plugin.config.getConfigurationSection("punishedPlayers")
            ?: plugin.config.createSection("punishedPlayers")

    fun punish(player: Player) {
        val endTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000)

        punished.set(player.name, endTime)
        plugin.saveConfig()

        player.gameMode = GameMode.SPECTATOR
        player.sendMessage("§cYou must wait 24 hours to respawn.")
    }

    fun unpunish(player: Player) {
        punished.set(player.name, null)
        plugin.saveConfig()

        player.gameMode = GameMode.SURVIVAL
        player.sendMessage("§aYou have been revived.")
    }

    fun checkTimers() {
        for (name in punished.getKeys(false)) {
            val end = punished.getLong(name)
            if (end <= System.currentTimeMillis()) {
                val p = Bukkit.getPlayer(name)
                if (p != null) unpunish(p)
            }
        }
    }

    fun getAllPunishedPlayers(): List<String> {
        return punished.getKeys(false)
            .filter { punished.getLong(it, 0L) > System.currentTimeMillis() }
    }

    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val damage = player.lastDamageCause ?: return
        val mob = plugin.playerMobMap[player.uniqueId]?.uppercase() ?: "NONE"
        if (damage !is EntityDamageByEntityEvent) return
        if (damage.damageSource.causingEntity !is Player || damage.damageSource.causingEntity == player) return
        if (mob == "TUFFGOLEM" || mob == "AXOLOTL" || mob == "NONE") return
        punish(player)
    }
}
