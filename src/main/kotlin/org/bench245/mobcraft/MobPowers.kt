package org.bench245.mobcraft.command.MobCraft.MobPowers

import org.bench245.mobcraft.Mobcraft
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.SmallFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

class MobPowers(private val plugin: Mobcraft) : Listener, CommandExecutor, TabCompleter {


    // Fire a small non-explosive fireball only when right-clicking with a Blaze Rod
    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        val player = event.player
        val actionName = event.action.name
        if (!actionName.contains("RIGHT_CLICK")) return

        val item = player.inventory.itemInMainHand
        if (item.type != Material.BLAZE_ROD) return

        // Launch SmallFireball (non-explosive, will ignite targets)
        val fireball = player.launchProjectile(SmallFireball::class.java)
        // SmallFireball doesn't always expose setIsIncendiary in older APIs — try reflection fallback:
        try {
            // In modern Paper APIs SmallFireball has setIsIncendiary(Boolean)
            val method = fireball::class.java.methods.firstOrNull { it.name == "setIsIncendiary" && it.parameterCount == 1 }
            method?.invoke(fireball, true as java.lang.Boolean)
        } catch (_: Throwable) {
            // ignore if not present
        }
        // Ensure no block-exploding behaviour (yield 0)
        try {
            val yieldMethod = fireball::class.java.getMethod("setYield", Float::class.javaPrimitiveType)
            yieldMethod.invoke(fireball, 0f)
        } catch (_: Throwable) {
            // ignore if not present
        }

        player.world.playSound(player.location, org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1f, 1f)
        player.sendMessage("${ChatColor.RED}🔥 Fireball launched! 🔥")
    }

    // Add +2 melee damage for Blaze players (applied on hit)
    @EventHandler
    fun onPlayerHit(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        event.damage += 2.0
        damager.sendMessage("${ChatColor.RED}🔥 Blaze power adds +2 damage! 🔥")
    }

}

