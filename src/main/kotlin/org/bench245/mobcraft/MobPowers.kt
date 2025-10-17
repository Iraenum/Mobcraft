package org.bench245.mobcraft.command.MobCraft.MobPowers

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent

class MobPowers(private val plugin: Mobcraft) {

    //Blaze
    fun onBlazeInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("BLAZE")
        plugin.enableFlight(player)
        player.flySpeed = 0.08F
    }
    // Fire a small non-explosive fireball only when right-clicking with a Blaze Rod
    fun onBlazeRightClick(event: PlayerInteractEvent) {
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
            val method =
                fireball::class.java.methods.firstOrNull { it.name == "setIsIncendiary" && it.parameterCount == 1 }
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
    }
    fun onBlazeHit(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        event.damage += 2.0
    }

    //Enderman
    fun onEndermanInitialize(player: Player) {
        applyEndermanSpeed(player)
        plugin.mobsToPreventLoot.add("ENDERMAN")
    }
    fun applyEndermanSpeed(player: Player) {
        val attribute = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)
        if (attribute != null) {
            // Default player speed = 0.1, so +0.05 is a 50% boost
            attribute.baseValue = 0.14
        }
    }
    fun onEndermanJoin(event: PlayerJoinEvent) {
        applyEndermanSpeed(event.player)
    }
    fun onEndermanRespawn(event: PlayerRespawnEvent) {
        applyEndermanSpeed(event.player)
    }
}

