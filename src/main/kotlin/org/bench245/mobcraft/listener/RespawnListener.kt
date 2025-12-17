package org.bench245.mobcraft.listener

import org.bench245.mobcraft.data.PunishmentManager
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

class RespawnListener(private val punish: PunishmentManager) : Listener {

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        if (punish.isPunished(player)) {
            player.gameMode = GameMode.SPECTATOR
            player.sendMessage("§cYou Still Have Time Left Before You Can Respawn!")
        }
    }
}
