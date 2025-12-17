package org.bench245.mobcraft.listener

import org.bench245.mobcraft.Mobcraft
import org.bench245.mobcraft.data.PunishmentManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.entity.Player

class DeathListener(private val plugin: Mobcraft, private val punish: PunishmentManager) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val damage = victim.lastDamageCause ?: return

        if (damage is EntityDamageByEntityEvent) {
            val killer = damage.damager
            if (killer is Player) {
                punish.punish(victim)
            }
        }
    }
}
