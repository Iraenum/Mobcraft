package org.bench245.mobcraft.command.MobCraft.MobPowers

import org.bench245.mobcraft.Mobcraft
import org.bukkit.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.*

class MobPowers(private val plugin: Mobcraft) {

    private val dragonAbilityCooldown = mutableSetOf<Player>()

    private val DRAGON_EGG_KEY = NamespacedKey(plugin, "bound_dragon_egg")

    private val eggOwners = WeakHashMap<Item, UUID>()

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
            PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false)
        )
    }

    fun onBlazeRightClick(event: PlayerInteractEvent) {
        onBlazeRodUse(event)
    }

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
                PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, false, false)
            )
        }

        if (!player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, false, false)
            )
        }
    }

    private fun enterBlazeCombatPhase(player: Player) {

        if (blazeCombatActive.contains(player.uniqueId)) return

        if (blazeCooldown.contains(player.uniqueId)) {
            player.sendMessage("§cYou must wait before entering combat phase again!")
            return
        }

        blazeCombatActive.add(player.uniqueId)
        blazeCooldown.add(player.uniqueId) // cooldown starts NOW

        player.sendMessage("§6You enter Blaze combat phase!")

        // Strength I for 10 seconds
        player.addPotionEffect(
            PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, false, false)
        )

        val particleTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!blazeCombatActive.contains(player.uniqueId)) return@Runnable

            player.world.spawnParticle(
                Particle.SMOKE,
                player.location.clone().add(0.0, 1.0, 0.0),
                20, 0.4, 0.6, 0.4, 0.01
            )

            player.world.spawnParticle(
                Particle.FLAME,
                player.location.clone().add(0.0, 1.0, 0.0),
                8, 0.3, 0.4, 0.3, 0.0
            )
        }, 0L, 1L)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {

            blazeCombatActive.remove(player.uniqueId)
            player.removePotionEffect(PotionEffectType.STRENGTH)
            particleTask.cancel()

            player.sendMessage("§7Combat phase ended. Cooling down...")

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                blazeCooldown.remove(player.uniqueId)
                player.sendMessage("§aYou may enter combat phase again.")
            }, 200L)

        }, 200L)
    }

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

    fun canPierceEndermanShield(shooter: Any?): Boolean {
        return shooter is Blaze || shooter is EnderDragon
    }

    private val REFLECTED_KEY = NamespacedKey(plugin, "enderman_reflected")
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

    fun onEndermanLeftClick(event: PlayerInteractEvent) {
        val player = event.player
        if (plugin.playerMobMap[player]?.equals("ENDERMAN", true) != true) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return
        if (player.inventory.itemInMainHand.type != Material.ENDER_PEARL) return
        event.isCancelled = true

        val now = System.currentTimeMillis()
        val lastUse = enderPearlCooldowns[player] ?: 0
        if (now - lastUse < 1000) return
        enderPearlCooldowns[player] = now

        val targetBlock = player.getTargetBlockExact(128) ?: return
        var safeLoc = targetBlock.location.add(0.0, 1.0, 0.0)

        repeat(7) {
            val feet = safeLoc.block
            val head = safeLoc.clone().add(0.0, 1.0, 0.0).block
            if (!feet.type.isSolid && !head.type.isSolid) return@repeat
            safeLoc.add(0.0, 1.0, 0.0)
        }

        safeLoc.yaw = player.location.yaw
        safeLoc.pitch = player.location.pitch

        player.teleport(safeLoc)
        player.world.playSound(safeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
        player.world.spawnParticle(Particle.PORTAL, safeLoc, 30, 0.5, 1.0, 0.5, 0.2)
    }
    fun onEndermanProjectileDamage(event: EntityDamageByEntityEvent) {
        val target = event.entity as? Player ?: return
        val mob = plugin.playerMobMap[target]?.uppercase() ?: return
        if (mob != "ENDERMAN") return

        val projectile = event.damager as? Projectile ?: return
        if (projectile !is Arrow) return

        if (projectile.persistentDataContainer.has(REFLECTED_KEY, PersistentDataType.BYTE)) {
            return
        }

        if (projectile.shooter is Player &&
            (projectile.shooter as Player).name == "MercilessRattler"
        ) {
            return
        }

        event.isCancelled = true

        projectile.persistentDataContainer.set(
            REFLECTED_KEY,
            PersistentDataType.BYTE,
            1
        )

        projectile.velocity = projectile.velocity.multiply(-1)

        target.world.spawnParticle(
            Particle.CRIT,
            target.location.add(0.0, 1.0, 0.0),
            15, 0.3, 0.3, 0.3, 0.05
        )

        target.world.playSound(
            target.location,
            Sound.ITEM_SHIELD_BLOCK,
            1f,
            1.3f
        )
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

    fun applyDragonEffects(player: Player) {
        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false)
            )
        }

        if (!player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0, false, false, false)
            )
        }
    }

    @EventHandler
    fun onEnderDragonPotionDamage(event: EntityPotionEffectEvent) {
        val player = event.entity as? Player ?: return
        val mob = plugin.playerMobMap[player]?.uppercase() ?: return
        if (mob != "ENDER_DRAGON") return

        when (event.modifiedType) {
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.WITHER,
            PotionEffectType.BLINDNESS,
            PotionEffectType.SLOWNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.WEAKNESS -> event.isCancelled = true

        }
    }

    fun onDragonEggDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        val meta = item.itemMeta ?: return
        if (meta.persistentDataContainer.has(DRAGON_EGG_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            event.isCancelled = true
            event.player.sendMessage("§cYou cannot drop a Dragon Egg.")
        }
    }

    fun onDragonEggDeath(event: PlayerDeathEvent) {
        val player = event.entity

        val iterator = event.drops.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val meta = item.itemMeta ?: continue
            if (meta.persistentDataContainer.has(DRAGON_EGG_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
                iterator.remove()
                val dropped = player.world.dropItemNaturally(player.location, item)
                eggOwners[dropped] = player.uniqueId
            }
        }
    }

    fun onDragonEggPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item = event.item
        if (!eggOwners.containsKey(item)) return

        val ownerId = eggOwners[item] ?: return
        if (player.uniqueId != ownerId) {
            event.isCancelled = true
            player.sendMessage("§cYou cannot pick up this Dragon Egg.")
        } else {
            eggOwners.remove(item)
        }
    }

    fun onEnderDragonRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (player.inventory.itemInMainHand.type != Material.DRAGON_BREATH) return
        val mob = plugin.playerMobMap[player]?.uppercase() ?: return
        if (mob != "ENDER_DRAGON") return
        if (!event.action.name.contains("RIGHT_CLICK")) return

        if (dragonAbilityCooldown.contains(player)) {
            player.sendMessage("§cYour purple fireball is recharging!")
            return
        }
        dragonAbilityCooldown.add(player)
        Bukkit.getScheduler().runTaskLater(plugin as org.bukkit.plugin.java.JavaPlugin, Runnable {
            dragonAbilityCooldown.remove(player)
            player.sendMessage("§aPurple fireball ready!")
        }, 60L)

        shootPurpleFireball(player)
        event.isCancelled = true
    }

    private fun shootPurpleFireball(player: Player) {
        val world = player.world
        val eyeLocation = player.eyeLocation
        val lookDirection = eyeLocation.direction.normalize()

        val fireball = world.spawn(
            eyeLocation.add(lookDirection.multiply(1.0)),
            Fireball::class.java
        ) {
            it.setIsIncendiary(true)
            it.yield = 10f
            it.direction = lookDirection
            it.velocity = lookDirection.multiply(2)
            it.shooter = player
        }

        Bukkit.getScheduler().runTaskLater(plugin as org.bukkit.plugin.java.JavaPlugin, Runnable {
            dragonAbilityCooldown.remove(player)
            player.sendMessage("§aFireball ready!")
        }, 60L)
    }

    fun updateEnderDragonBeams() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val mob = plugin.playerMobMap[player]?.uppercase() ?: return@forEach
            if (mob != "ENDER_DRAGON") return@forEach

            val crystals = player.getNearbyEntities(100.0, 100.0, 100.0)
                .filterIsInstance<EnderCrystal>()

            if (crystals.isEmpty()) return@forEach

            crystals.forEach { crystal ->
                sendRealCrystalBeam(crystal, player)

                player.addPotionEffect(
                    PotionEffect(PotionEffectType.REGENERATION, 20, 1, true, false, false)
                )
            }
        }
    }

    fun sendRealCrystalBeam(crystal: EnderCrystal, player: Player) {
        val target = player.location.clone().add(0.0, 1.0, 0.0)
        crystal.beamTarget = target
    }

    fun onDragonFallDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        val mob = plugin.playerMobMap[player]?.uppercase() ?: return
        if (mob == "ENDER_DRAGON" && event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }

    fun onEnderDragonBreak(event: BlockBreakEvent) {
        val player = event.player
        if (event.block.type != Material.END_PORTAL_FRAME) return
        val key = strongholdKey(event.block.location)
        val visited = getVisitedStrongholds(player)

        if (visited.contains(key)) {
            player.sendMessage("§dYou shatter the End Portal Frame with your ancient might!")
            rememberStronghold(player, key)
        } else {
            player.sendMessage("§4This portal is foreign to you... you cannot destroy it yet.")
            event.isCancelled = true
        }
    }

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
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.RESISTANCE,
                Int.MAX_VALUE,
                3, // Resistance IV
                false,
                false,
                false
            )
        )
        player.getAttribute(Attribute.SCALE)?.baseValue = 0.5
    }

    fun onTuffGolemHit(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return
        if (plugin.playerMobMap[player] != "TUFFGOLEM") return
    }

    fun onTuffGolemDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (plugin.playerMobMap[player] == "TUFFGOLEM") {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                player.spigot().respawn()
            }, 1L)
        }
    }

    fun applyGolemEffects(player: Player) {
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.RESISTANCE,
                Int.MAX_VALUE,
                3,
                false,
                false,
                false
            )
        )
    }

    fun openTuffGolemEnderChest(player: Player) {
        if (plugin.playerMobMap[player] == "TUFFGOLEM") {
            player.openInventory(player.enderChest)
        } else {
            player.sendMessage("§cOnly Tuff Golems can use this command!")
        }
    }

    // ----------------------- GHAST -------------------------------
    private val ghastFireballCooldowns = mutableMapOf<Player, Long>()

    fun onGhastInitialize(player: Player) {
        plugin.mobsToPreventLoot.add("GHAST")
        plugin.enableFlight(player)
        player.flySpeed = 0.08F
        applyGhastEffects(player)
        player.addPotionEffect(
            PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false))
    }

    fun applyGhastEffects(player: Player) {
        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, false, false, false)
            )
        }
    }

    fun onGhastFireball(event: PlayerInteractEvent) {
        val player = event.player
        if (plugin.playerMobMap[player]?.uppercase() != "GHAST") return
        if (!event.action.name.contains("RIGHT_CLICK")) return

        val item = player.inventory.itemInMainHand.type
        if (item != Material.GHAST_TEAR && item != Material.GUNPOWDER) return

        val now = System.currentTimeMillis()
        val lastShoot = ghastFireballCooldowns[player] ?: 0
        if (now - lastShoot < 1000) return
        ghastFireballCooldowns[player] = now

        val direction = player.location.direction.normalize()
        val fireball = player.world.spawn(player.eyeLocation.add(direction), Fireball::class.java)
        fireball.velocity = direction.multiply(0.25)
        fireball.yield = 4f
        fireball.setIsIncendiary(true)

        val blockBelow = player.location.clone().add(0.0, -1.0, 0.0).block
        if (!blockBelow.isPassable) {
            player.velocity = player.velocity.clone().add(Vector(0.0, 2.5, 0.0))
            player.world.playSound(player.location, Sound.ENTITY_GHAST_WARN, 1f, 0.8f)
        }

        player.world.playSound(player.location, "minecraft:entity.ghast.shoot", 1f, 1f)
    }

    fun onGhastDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (plugin.playerMobMap[player]?.uppercase() != "GHAST") return

        if (
            event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
            event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
            event.cause == EntityDamageEvent.DamageCause.FIRE ||
            event.cause == EntityDamageEvent.DamageCause.FIRE_TICK
        ) {
            event.isCancelled = true
        }
    }

    fun onGhastFireballHit(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return
        if (plugin.playerMobMap[player]?.uppercase() != "GHAST") return

        val fireball = event.damager as? Fireball
        if (fireball != null && fireball.shooter == player) {
            event.isCancelled = true
        }
    }

    fun onGhastDrinkMilk(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (plugin.playerMobMap[player]?.uppercase() != "GHAST") return
        if (event.item.type != Material.MILK_BUCKET) return

        Bukkit.getScheduler().runTask(plugin, Runnable {
            applyGhastEffects(player)
        })
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

    fun applyAxolotlEffects(player: Player) {
        if (!player.hasPotionEffect(PotionEffectType.REGENERATION)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.REGENERATION, Int.MAX_VALUE, 0, false, false, false)
            )
        }

        if (!player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0, false, false, false)
            )
        }
        if (!player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.DOLPHINS_GRACE, Int.MAX_VALUE, 0, false, false, false)
            )
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

            val fireball = player.world.spawn(
                player.eyeLocation.add(player.location.direction.multiply(1.0)),
                SmallFireball::class.java
            )
            fireball.direction = player.location.direction.multiply(1.5)
            fireball.yield = 1.5f
            fireball.shooter = player
            player.world.playSound(player.location, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1f)
            player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.5, 0.0), 25, 0.2, 0.2, 0.2, 0.01)
        }

        // ----------------------- SKELETON -----------------------
        fun onSkeletonInitialize(player: Player) {
            plugin.mobsToPreventLoot.add("SKELETON")

            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.POISON,
                    1,
                    0,
                    false,
                    false
                )
            ) // just to clear if present
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
                    arrow.velocity =
                        arrow.velocity.add(direction.multiply(0.2)).normalize().multiply(arrow.velocity.length())
                }
            }.runTaskTimer(plugin, 1L, 1L)
        }
    }
}
