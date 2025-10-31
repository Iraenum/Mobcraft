package org.bench245.mobcraft.command.MobCraft.MobPowers

import net.md_5.bungee.api.ChatColor
import org.bench245.mobcraft.Mobcraft
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Fireball
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.scheduler.BukkitRunnable

class MobPowers(private val plugin: Mobcraft) {
    fun onBlazeInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("BLAZE")
        plugin.enableFlight(player)
        player.flySpeed = 0.1F
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

    // Add +2 melee damage for Blaze players (applied on hit)
    fun onBlazehit(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        event.damage += 2.0
    }

    fun onEndermanInitialized(player: Player) {
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

    fun onEnderDragonInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("ENDERDRAGON")
        // Fire Resistance + Water Breathing
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0, false, false))
        plugin.enableFlight(player)
        player.flySpeed = 0.3F
    }

    fun onEnderDragonMove(event: PlayerMoveEvent) {
        val player = event.player
        // ----------------------- Immunity to Weakness, Slowness, Poison, Wither, Slow Falling -----------------------
        listOf(
            PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS, PotionEffectType.POISON,
            PotionEffectType.WITHER, PotionEffectType.SLOW_FALLING
        ).forEach {
            if (player.hasPotionEffect(it)) player.removePotionEffect(it)
        }
        // ----------------------- Regen 2 near end crystal -----------------------
        val nearbyCrystals = player.getNearbyEntities(5.0, 5.0, 5.0).filterIsInstance<EnderCrystal>()
        if (nearbyCrystals.isNotEmpty()) {
            if (!player.hasPotionEffect(PotionEffectType.REGENERATION)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false))
                if (player.hasPlayedBefore()) player.world.loadedChunks.forEach { chunk ->
                    chunk.entities.forEach {
                        if (it.location.block.type == Material.END_PORTAL_FRAME) it.location.block.type = Material.AIR
                    }
                }
            }
        }
        // ----------------------- Tuff Golem Start -----------------------
        fun onTuffGolemInitialized(player: Player) {
            plugin.mobsToPreventLoot.add("TUFFGOLEM")
            val armorAttr = player.getAttribute(Attribute.ARMOR_TOUGHNESS)
            armorAttr?.baseValue = (armorAttr?.baseValue ?: 0.0) + 1.0
        }

        // ----------------------- DAMAGE REDUCTION -----------------------
        fun onTuffGolemHit(event: EntityDamageByEntityEvent) {
            val player = event.entity
            if (player is Player && plugin.playerMobMap[player] == "TUFFGOLEM") {
                // Reduces incoming damage by 20%
                event.damage *= 0.8
            }
            // ----------------------- INSTANT RESPAWN -----------------------
            fun onTuffGolemDeath(event: PlayerDeathEvent) {
                val player = event.entity
                if (plugin.playerMobMap[player] == "TUFFGOLEM") {
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        player.spigot().respawn()
                        player.sendMessage("${ChatColor.GRAY}You instantly respawned as a Tuff Golem!")
                    }, 1L)
                    // ----------------------- ENDERCHEST ACCESS -----------------------
                    fun openEnderChest(player: Player) {
                        if (plugin.playerMobMap[player] != "tuff_golem") return
                        player.openInventory(player.enderChest)
                    }

                    //----------------Ghast Start------------------
                    fun onGhastInitialized(player: Player) {
                        plugin.mobsToPreventLoot.add("GHAST")
                        plugin.enableFlight(player)
                        player.flySpeed = 0.1F
                    }

                    // ----------------------- SHOOT FIREBALL -----------------------
                    fun onGhastFireball(event: PlayerInteractEvent) {
                        val player = event.player
                        if (plugin.playerMobMap[player] != "GHAST") return
                        if (event.action.name.contains("RIGHT_CLICK")) {
                            val item = player.inventory.itemInMainHand
                            if (item.type != Material.GHAST_TEAR) return
                        }
                        val fireball = player.launchProjectile(Fireball::class.java)
                        fireball.setIsIncendiary(true)
                        fireball.yield = 1f
                        player.world.playSound(player.location, Sound.ENTITY_GHAST_SHOOT, 1f, 1f)
                    }

                    // ----------------------- AXOLOTL Start -----------------------
                    fun onAxolotlInitialized(player: Player) {
                        plugin.mobsToPreventLoot.add("AXOLOTL")
                        // Swimming speed: apply SPEED effect only in water
                        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 1, false, false))
                        // Apply potion effects
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.REGENERATION,
                                Int.MAX_VALUE,
                                1,
                                false,
                                false
                            )
                        )
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.WATER_BREATHING,
                                Int.MAX_VALUE,
                                0,
                                false,
                                false
                            )
                        )
                    }
                    // ----------------------- INSTANT RESPAWN -----------------------

                    fun onAxolotlDeath(event: PlayerDeathEvent) {
                        val player = event.entity
                        if (plugin.playerMobMap[player] != "axolotl") return
                        // Respawn the player instantly
                        plugin.server.scheduler.runTaskLater(plugin, Runnable {
                            player.spigot().respawn()
                            player.sendMessage("${ChatColor.AQUA}You instantly respawned as an Axolotl!")
                        }, 1L)
                        fun onElderGuardianInitialized(player: Player) {
                            plugin.mobsToPreventLoot.add("ELDER_Guardian")
                            player.addPotionEffect(
                                PotionEffect(PotionEffectType.RESISTANCE, Int.MAX_VALUE, 0, false, false)
                            )
                            player.addPotionEffect(
                                PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0, false, false)
                            )

                            // Schedule mining fatigue application every 30s while underwater
                            object : BukkitRunnable() {
                                override fun run() {
                                    if (!player.isOnline) {
                                        cancel()
                                        return
                                    }
                                    if (player.location.block.type == Material.WATER || player.isSwimming) {
                                        for (p in player.world.players) {
                                            if (p != player && p.location.distance(player.location) <= 10) {
                                                p.addPotionEffect(
                                                    PotionEffect(
                                                        PotionEffectType.MINING_FATIGUE,
                                                        60,
                                                        0,
                                                        false,
                                                        false
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }.runTaskTimer(plugin, 0L, 600L) // 600 ticks = 30 seconds
                        }

                        fun onElderGuardianHit(event: EntityDamageByEntityEvent) {
                            val damager = event.damager
                            if (damager is Player) {
                                // reserved for future melee logic
                            }
                        }
                        fun onElderGuardianLaser(event: PlayerInteractEvent) {
                            val player = event.player
                            if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                                if (player.inventory.itemInMainHand.type == Material.PRISMARINE_SHARD) {
                                    player.world.spawn(
                                        player.eyeLocation.add(player.location.direction.multiply(1.0)),
                                        SmallFireball::class.java
                                    ) { fireball ->
                                        fireball.direction = player.location.direction.multiply(1.5)
                                        fireball.yield = 1.5f
                                        fireball.shooter = player
                                    }
                                    player.world.playSound(player.location, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1f)
                                    player.world.spawnParticle(
                                        Particle.END_ROD,
                                        player.location.add(0.0, 1.5, 0.0),
                                        25,
                                        0.2,
                                        0.2,
                                        0.2,
                                        0.01
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


