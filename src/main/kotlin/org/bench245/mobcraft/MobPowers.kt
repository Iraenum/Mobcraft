package org.bench245.mobcraft.command.MobCraft.MobPowers

import org.bench245.mobcraft.Mobcraft
import org.bukkit.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.EulerAngle
import java.util.UUID

class MobPowers(private val plugin: Mobcraft) {

    fun resetPlayerState(player: Player) {

        player.activePotionEffects.forEach {
            player.removePotionEffect(it.type)
        }

        player.walkSpeed = 0.2f
        player.flySpeed = 0.1f

        player.allowFlight = false
        player.isFlying = false

        player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.1
        player.getAttribute(Attribute.ARMOR)?.baseValue = 0.0
        player.getAttribute(Attribute.ARMOR_TOUGHNESS)?.baseValue = 0.0
        player.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = 1.0
        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0

        // Clamp health
        if (player.health > 20.0) {
            player.health = 20.0
        }
    }

    // ----------------------- BLAZE -------------------------------

    private val blazeCombatActive = mutableSetOf<UUID>()
    private val blazeCooldown = mutableSetOf<UUID>()

    fun onBlazeInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("BLAZE")
        plugin.enableFlight(player)
        player.flySpeed = 0.03F

        player.addPotionEffect(
            PotionEffect(PotionEffectType.STRENGTH, Int.MAX_VALUE, 1, false, false, false)
        )
        player.addPotionEffect(
            PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false)
        )
    }

    /**
     * REQUIRED BY Mobcraft.kt
     * Wrapper → routes to Blaze Rod logic
     */
    fun onBlazeRightClick(event: PlayerInteractEvent) {
        onBlazeRodUse(event)
    }

    /**
     * REQUIRED BY Mobcraft.kt
     * Passive upkeep (NO hover)
     */
    fun onBlazeMove(event: PlayerMoveEvent) {
        val player = event.player
        val mob = plugin.playerMobMap[player]?.uppercase() ?: return
        if (mob != "BLAZE") return

        // Re-apply fire resistance safely
        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false)
            )
        }
    }

    /**
     * Blaze Rod interactions
     * LEFT CLICK = Enter Smoky Combat Phase
     * RIGHT CLICK = Shoot fireball (combat phase only)
     */
    fun onBlazeRodUse(event: PlayerInteractEvent) {
        val player = event.player
        if (player.inventory.itemInMainHand.type != Material.BLAZE_ROD) return

        val mob = plugin.playerMobMap[player]?.uppercase() ?: return
        if (mob != "BLAZE") return

        // LEFT CLICK → Enter combat phase
        if (event.action.name.contains("LEFT_CLICK")) {
            if (blazeCombatActive.contains(player.uniqueId)) return

            if (blazeCooldown.contains(player.uniqueId)) {
                player.sendMessage("§cYou must wait before entering combat phase again!")
                return
            }

            enterBlazeCombatPhase(player)
            return
        }

        // RIGHT CLICK → Shoot fireball
        if (event.action.name.contains("RIGHT_CLICK")) {
            if (!blazeCombatActive.contains(player.uniqueId)) {
                player.sendMessage("§cYou can only shoot fireballs during combat phase!")
                return
            }

            val fireball = player.launchProjectile(SmallFireball::class.java)
            fireball.setVisualFire(true)
            fireball.yield = 0f

            player.world.playSound(
                player.location,
                Sound.ENTITY_BLAZE_SHOOT,
                1f,
                1f
            )
        }
    }
    fun applyBlazeEffects(player: Player) {
        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false)
            )
        }

        if (!player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.STRENGTH, Int.MAX_VALUE, 1, false, false, false)
            )
        }
    }

    private fun enterBlazeCombatPhase(player: Player) {
        blazeCombatActive.add(player.uniqueId)
        player.sendMessage("§6You enter Blaze combat phase!")

        // Smoke + flame visuals (10s)
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!blazeCombatActive.contains(player.uniqueId)) return@Runnable

            player.world.spawnParticle(
                Particle.SMOKE,
                player.location.clone().add(0.0, 1.0, 0.0),
                20,
                0.4,
                0.6,
                0.4,
                0.01
            )

            player.world.spawnParticle(
                Particle.FLAME,
                player.location.clone().add(0.0, 1.0, 0.0),
                8,
                0.3,
                0.4,
                0.3,
                0.0
            )
        }, 0L, 1L)

        // End combat after 10s
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            blazeCombatActive.remove(player.uniqueId)
            task.cancel()

            player.sendMessage("§7Combat phase ended. Cooling down...")
            blazeCooldown.add(player.uniqueId)

            // Cooldown ends after 10s
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                blazeCooldown.remove(player.uniqueId)
                player.sendMessage("§aYou may enter combat phase again.")
            }, 200L)

        }, 200L)
    }

    /**
     * Projectile reflection (ONLY during Smoky phase)
     */
    fun onBlazeProjectileHit(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return
        val damager = event.damager

        val mob = plugin.playerMobMap[player]?.uppercase() ?: return
        if (mob != "BLAZE") return
        if (!blazeCombatActive.contains(player.uniqueId)) return

        if (damager is Projectile) {
            event.isCancelled = true
            damager.velocity = damager.velocity.multiply(-1)

            player.world.playSound(
                player.location,
                Sound.ENTITY_BLAZE_HURT,
                1f,
                1.2f
            )
        }
    }

    /**
     * Cleanup
     */
    fun onBlazeRespawn(event: PlayerRespawnEvent) {
        resetBlaze(event.player)
    }

    fun onBlazeGamemodeChange(event: PlayerGameModeChangeEvent) {
        if (event.newGameMode == GameMode.SPECTATOR) {
            resetBlaze(event.player)
        }
    }

    private fun resetBlaze(player: Player) {
        blazeCombatActive.remove(player.uniqueId)
        blazeCooldown.remove(player.uniqueId)
    }
    // ----------------------- ENDERMAN -------------------------------
    private val enderPearlCooldowns = mutableMapOf<Player, Long>()

    fun onEndermanInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("ENDERMAN")
        applyEndermanSpeed(player)
    }

    fun applyEndermanSpeed(player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.14
    }

    fun onEndermanJoin(event: PlayerJoinEvent) = applyEndermanSpeed(event.player)
    fun onEndermanRespawn(event: PlayerRespawnEvent) = applyEndermanSpeed(event.player)

    fun onEndermanRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (plugin.playerMobMap[player]?.equals("ENDERMAN", ignoreCase = true) != true) return
        if (!event.action.name.contains("RIGHT_CLICK")) return
        if (player.inventory.itemInMainHand.type != Material.ENDER_PEARL) return

        event.isCancelled = true

        val now = System.currentTimeMillis()
        val lastUse = enderPearlCooldowns[player] ?: 0
        if (now - lastUse < 1000) return
        enderPearlCooldowns[player] = now

        val targetBlock = player.getTargetBlockExact(128) ?: return
        val targetLoc = targetBlock.location.add(0.0, 1.0, 0.0)
        var safeLoc = targetLoc.clone()

        for (i in 0..6) {
            val feet = safeLoc.block
            val head = safeLoc.clone().add(0.0, 1.0, 0.0).block
            if (!feet.type.isSolid && !head.type.isSolid) break
            safeLoc.add(0.0, 1.0, 0.0)
        }
        safeLoc.yaw = player.location.yaw
        safeLoc.pitch = player.location.pitch

        player.teleport(safeLoc)

        player.world.playSound(safeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
        player.world.spawnParticle(Particle.PORTAL, safeLoc, 30, 0.5, 1.0, 0.5, 0.2)
    }
    fun onEndermanProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        val hitEntity = event.hitEntity ?: return
        if (hitEntity !is Player) return

        val mob = plugin.playerMobMap[hitEntity]?.uppercase() ?: return
        if (mob != "ENDERMAN") return
        if (projectile !is Arrow &&
            projectile !is ThrownPotion &&
            projectile !is LingeringPotion) return

        val shooter = (projectile.shooter as? LivingEntity)

        if (shooter is Player && shooter.name == "MercilessRattler") {
            return
        }
        val loc = projectile.location
        val reverseVel = projectile.velocity.clone().multiply(-1.0)

        when (projectile) {
            is Arrow -> {
                val newArrow = projectile.world.spawn(loc, Arrow::class.java)
                newArrow.shooter = shooter as? LivingEntity
                newArrow.velocity = reverseVel
                try {
                    newArrow.pickupRule = AbstractArrow.PickupRule.DISALLOWED
                } catch (_: Throwable) { /* fallback if API differs */ }
                projectile.remove()
            }
            is ThrownPotion, is LingeringPotion -> {
                val itemStack: ItemStack? = try {
                    (projectile as? ThrownPotion)?.item
                } catch (_: Throwable) { null }

                val spawned = projectile.world.spawn(loc, ThrownPotion::class.java)
                if (spawned != null) {
                    if (itemStack != null) {
                        try { spawned.item = itemStack } catch (_: Throwable) { /* ignore if setter missing */ }
                    }
                    spawned.shooter = shooter
                    spawned.velocity = reverseVel
                }
                projectile.remove()
            }
        }
        hitEntity.world.spawnParticle(
            org.bukkit.Particle.CRIT,
            hitEntity.location.add(0.0, 1.0, 0.0),
            15, 0.3, 0.3, 0.3, 0.05
        )
        hitEntity.world.playSound(hitEntity.location, org.bukkit.Sound.ITEM_SHIELD_BLOCK, 1f, 1.3f)

        // cancel default handling on the original hit
        event.isCancelled = true
    }

    fun onEndermanProjectileDamage(event: EntityDamageByEntityEvent) {
        val target = event.entity as? Player ?: return
        val mob = plugin.playerMobMap[target]?.uppercase() ?: return
        if (mob != "ENDERMAN") return

        val damager = event.damager
        // only care about projectile-based damage
        val isRelevant = damager is Arrow || damager is ThrownPotion || damager is LingeringPotion
        if (!isRelevant) return
        val shooter = (damager as? Projectile)?.shooter
        if (shooter is Player && shooter.name == "MercilessRattler") {
            return
        }
        event.isCancelled = true
        try {
            val proj = damager as Projectile
            val reverse = proj.velocity.clone().multiply(-1.0)
            if (proj is Arrow) {
                val newArrow = proj.world.spawn(proj.location, Arrow::class.java)
                newArrow.shooter = shooter as? LivingEntity
                newArrow.velocity = reverse
                try { newArrow.pickupRule = AbstractArrow.PickupRule.DISALLOWED } catch (_: Throwable) {}
                proj.remove()
            } else if (proj is ThrownPotion || proj is LingeringPotion) {
                val itemStack: ItemStack? = try { (proj as? ThrownPotion)?.item } catch (_: Throwable) { null }
                val spawned = proj.world.spawn(proj.location, ThrownPotion::class.java)
                if (spawned != null) {
                    if (itemStack != null) {
                        try { spawned.item = itemStack } catch (_: Throwable) {}
                    }
                    spawned.shooter = shooter as? LivingEntity
                    spawned.velocity = reverse
                }
                proj.remove()
            } else {
                proj.remove()
            }
        } catch (_: Throwable) {
        }

        target.world.spawnParticle(
            org.bukkit.Particle.CRIT,
            target.location.add(0.0, 1.0, 0.0),
            15, 0.3, 0.3, 0.3, 0.05
        )
        target.world.playSound(target.location, org.bukkit.Sound.ITEM_SHIELD_BLOCK, 1f, 1.3f)
    }
    // ----------------------- ENDER DRAGON -------------------------

    fun onEnderDragonInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("ENDER_DRAGON")
        plugin.enableFlight(player)
        player.flySpeed = 0.1F

        // Permanent buffs
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0))
    }

    /**
     * Must be run every tick:
     *
     * Bukkit.getScheduler().runTaskTimer(plugin, { updateEnderDragonBeams() }, 1L, 1L)
     */
    fun updateEnderDragonBeams() {
        Bukkit.getOnlinePlayers().forEach { player ->

            val mob = plugin.playerMobMap[player]?.uppercase() ?: return@forEach
            if (mob != "ENDER_DRAGON") return@forEach

            // Search for end crystals in a 100-block cube
            val crystals = player.getNearbyEntities(100.0, 100.0, 100.0)
                .filterIsInstance<EnderCrystal>()

            if (crystals.isEmpty()) return@forEach

            crystals.forEach { crystal ->
                sendRealCrystalBeam(crystal, player)

                // Apply regeneration 100% reliably
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.REGENERATION,
                        20,   // 1 second
                        1,    // amplifier
                        true,
                        false,
                        false
                    )
                )
            }
        }
    }

    /**
     * Apply the real vanilla end crystal beam
     */
    fun sendRealCrystalBeam(crystal: EnderCrystal, player: Player) {
        // Beam MUST point to an integer block location
        val target = player.location.clone().add(0.0, 1.0, 0.0) // centers the beam on player's chest
        crystal.beamTarget = target
    }

    /**
     * Ender Dragon cannot take fall damage
     */
    fun onDragonFallDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player

        val mob = plugin.playerMobMap[player]?.uppercase() ?: return

        if (mob == "ENDER_DRAGON" && event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }

    /**
     * Allows breaking End Portal Frames only in visited strongholds.
     */
    fun onEnderDragonBreak(event: BlockBreakEvent) {
        val player = event.player
        if (event.block.type != Material.END_PORTAL_FRAME) return

        val key = strongholdKey(event.block.location)
        val visited = getVisitedStrongholds(player)

        if (visited.contains(key)) {
            player.sendMessage("§dYou shatter the End Portal Frame with your ancient might!")
        } else {
            player.sendMessage("§4This portal is foreign to you... you cannot destroy it yet.")
            event.isCancelled = true
        }
    }

    /**
     * Store stronghold key in persistent data
     */
    private fun rememberStronghold(player: Player, key: String) {
        val data = player.persistentDataContainer
        val strongholdKey = NamespacedKey(plugin, "visited_strongholds")
        val current = data.get(strongholdKey, PersistentDataType.STRING) ?: ""
        if (!current.contains(key)) {
            val updated = if (current.isEmpty()) key else "$current,$key"
            data.set(strongholdKey, PersistentDataType.STRING, updated)
        }
    }

    private fun getVisitedStrongholds(player: Player): List<String> {
        val data = player.persistentDataContainer
        val strongholdKey = NamespacedKey(plugin, "visited_strongholds")
        val stored = data.get(strongholdKey, PersistentDataType.STRING) ?: ""
        return stored.split(",").filter { it.isNotBlank() }
    }

    private fun isInStronghold(loc: Location): Boolean {
        val world = loc.world ?: return false
        val nearbyFrames = (loc.blockY - 16..loc.blockY + 16).flatMap { y ->
            (-16..16).flatMap { dx ->
                (-16..16).mapNotNull { dz ->
                    val block = world.getBlockAt(loc.blockX + dx, y, loc.blockZ + dz)
                    if (block.type == Material.END_PORTAL_FRAME) block else null
                }
            }
        }
        return nearbyFrames.size >= 3
    }

    private fun strongholdKey(loc: Location): String {
        val chunk = loc.chunk
        return "${chunk.world.name}_${chunk.x shr 2}_${chunk.z shr 2}"
    }
    // ----------------------- TUFF GOLEM ---------------------------
    fun onTuffGolemInitialize(player: Player) {
        player.getAttribute(Attribute.ARMOR_TOUGHNESS)?.baseValue =
            (player.getAttribute(Attribute.ARMOR_TOUGHNESS)?.baseValue ?: 0.0) + 1.0
        player.getAttribute(Attribute.SCALE)?.baseValue ?:0.7
    }
    fun onTuffGolemHit(event: EntityDamageByEntityEvent) {
        val player = event.entity
        if (player is Player && plugin.playerMobMap[player] == "TUFFGOLEM") {
            event.damage *= 10 // 20% damage reduction
        }
    }
    fun onTuffGolemDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (plugin.playerMobMap[player] == "TUFFGOLEM") {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                player.spigot().respawn()
            }, 1L)
        }
    }
    fun openTuffGolemEnderChest(player: Player) {
        if (plugin.playerMobMap[player] == "TUFFGOLEM") {
            player.openInventory(player.enderChest)
        } else {
            player.sendMessage("§cOnly Tuff Golems can use this command!")
        }
    }

    fun showFakeEnderChest(player: Player) {
        val plugin = this.plugin
        player.world.entities
            .filter { it.customName == "TUFFGOLEM_FAKE_CHEST_${player.uniqueId}" }
            .forEach { it.remove() }
        val stand = player.world.spawn(player.location.add(0.0, 1.5, 0.0), ArmorStand::class.java) { asEntity ->
            asEntity.isVisible = false
            asEntity.isMarker = true
            asEntity.setGravity(false)
            asEntity.setCustomName("TUFFGOLEM_FAKE_CHEST_${player.uniqueId}")
            asEntity.setCustomNameVisible(false)
            asEntity.equipment?.setItemInMainHand(ItemStack(Material.ENDER_CHEST))
            asEntity.equipment?.setItemInOffHand(ItemStack(Material.ENDER_CHEST))
            asEntity.rightArmPose = EulerAngle(-0.5, 0.0, 0.0)
            asEntity.leftArmPose = EulerAngle(-0.5, 0.0, 0.0)
        }

        // Task to follow the player
        object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || player.isDead) {
                    stand.remove()
                    cancel()
                    return
                }
                stand.teleport(player.location.add(0.0, 1.5, 0.0))
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
    /// ----------------------- GHAST -------------------------------
    private val ghastFireballCooldowns = mutableMapOf<Player, Long>() // store last shoot time

    fun onGhastInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("GHAST")
        plugin.enableFlight(player)
        player.flySpeed = 0.03F

        // Give infinite fire resistance
        player.addPotionEffect(
            PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 1, false, false, false)
        )
    }
    fun onGhastFireball(event: PlayerInteractEvent) {
        val player = event.player

        if (plugin.playerMobMap[player]?.uppercase() != "GHAST") return
        if (!event.action.name.contains("RIGHT_CLICK")) return

        val item = player.inventory.itemInMainHand.type
        if (item != Material.GHAST_TEAR && item != Material.GUNPOWDER) return

        // 1 second cooldown
        val now = System.currentTimeMillis()
        val lastShoot = ghastFireballCooldowns[player] ?: 0
        if (now - lastShoot < 1000) return
        ghastFireballCooldowns[player] = now

        val direction = player.location.direction.normalize().multiply(0.25) // slower than blaze
        val fireball = player.world.spawn(player.eyeLocation.add(direction), Fireball::class.java)
        fireball.setIsIncendiary(true)
        fireball.velocity = direction
        fireball.yield = 4f
        fireball.isSilent = false
        fireball.shooter = player // shooter is known

        player.world.playSound(player.location, "minecraft:entity.ghast.shoot", 1f, 1f)
    }

    fun onGhastSelfDamage(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        val damager = event.damager

        if (entity !is Player) return
        if (damager is Fireball && damager.shooter == entity) {
            event.isCancelled = true
        }
    }

    fun onGhastExplosionDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return

        if (event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
            event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        ) {
            val lastDamager = (event as? EntityDamageByEntityEvent)?.damager
            if (lastDamager is Fireball && lastDamager.shooter == entity) {
                event.isCancelled = true
            }
        }
}
    // ----------------------- AXOLOTL -------------------------------

    fun onAxolotlInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("AXOLOTL")
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, Int.MAX_VALUE, 1))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false, false))

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.DOLPHINS_GRACE,
                Int.MAX_VALUE,
                0,
                false, false, false
            )
        )
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) return@Runnable

            val type = player.location.block.type
            val inWater =
                type == Material.WATER ||
                        type == Material.KELP ||
                        type == Material.KELP_PLANT ||
                        type == Material.BUBBLE_COLUMN
            if (inWater) {
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.CONDUIT_POWER,
                        40,
                        0,
                        false, false, false
                    )
                )
            } else {
                // Normal on land
                player.removePotionEffect(PotionEffectType.CONDUIT_POWER)
            }

        }, 0L, 10L)
    }
    fun onAxolotlDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (plugin.playerMobMap[player]?.uppercase() == "AXOLOTL") {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                player.spigot().respawn()
            }, 1L)
        }
    }
    // ----------------------- ELDER GUARDIAN -----------------------
    fun onElderGuardianInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("ELDER_GUARDIAN")
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, Int.MAX_VALUE, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0))

        object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    cancel()
                    return
                }
                if (player.location.block.type == Material.WATER || player.isSwimming) {
                    for (p in player.world.players) {
                        if (p != player && p.location.distance(player.location) <= 10) {
                            p.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0))
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 600L)
    }
    fun onElderGuardianLaser(event: PlayerInteractEvent) {
        val player = event.player
        if (!event.action.name.contains("RIGHT_CLICK")) return
        if (player.inventory.itemInMainHand.type != Material.PRISMARINE_SHARD) return

        val fireball = player.world.spawn(player.eyeLocation.add(player.location.direction.multiply(1.0)), SmallFireball::class.java)
        fireball.direction = player.location.direction.multiply(1.5)
        fireball.yield = 1.5f
        fireball.shooter = player
        player.world.playSound(player.location, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1f)
        player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.5, 0.0), 25, 0.2, 0.2, 0.2, 0.01)
    }
    // ----------------------- SKELETON -----------------------
    fun onSkeletonInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("SKELETON")

        player.addPotionEffect(PotionEffect(PotionEffectType.POISON, 1, 0, false, false)) // just to clear if present
        player.removePotionEffect(PotionEffectType.POISON)
    }
    fun onSkeletonTick(player: Player) {
        if (player.hasPotionEffect(PotionEffectType.POISON)) {
            player.removePotionEffect(PotionEffectType.POISON)
        }
    }
    // ----------------------- HOMING ARROWS -----------------------
    fun onSkeletonShoot(event: EntityShootBowEvent) {
        val player = event.entity
        if (player !is Player) return
        if (plugin.playerMobMap[player]?.equals("SKELETON", ignoreCase = true) != true) return

        val arrow = event.projectile
        if (arrow !is Arrow) return

        object : BukkitRunnable() {
            override fun run() {
                if (arrow.isDead || arrow.isOnGround) {
                    cancel()
                    return
                }
                val nearby = arrow.world.getNearbyEntities(arrow.location, 16.0, 16.0, 16.0)
                    .filterIsInstance<LivingEntity>()
                    .filter { it != player }

                val target = nearby.minByOrNull { it.location.distanceSquared(arrow.location) } ?: return
                val direction = target.eyeLocation.toVector().subtract(arrow.location.toVector()).normalize()
                arrow.velocity = arrow.velocity.add(direction.multiply(0.2)).normalize().multiply(arrow.velocity.length())
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }
}
