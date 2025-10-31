package org.bench245.mobcraft.command.MobCraft.MobPowers

import net.md_5.bungee.api.ChatColor
import org.bench245.mobcraft.Mobcraft
import org.bukkit.*
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

class MobPowers(private val plugin: Mobcraft) {

    // ----------------------- BLAZE -------------------------------
    fun onBlazeInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("BLAZE")
        plugin.enableFlight(player)
        player.flySpeed = 0.1F
    }
    fun onBlazeRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (!event.action.name.contains("RIGHT_CLICK")) return
        if (player.inventory.itemInMainHand.type != Material.BLAZE_ROD) return

        val fireball = player.launchProjectile(SmallFireball::class.java)
        (fireball as org.bukkit.entity.Fireball).setIsIncendiary(true)
        fireball.yield = 0f
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f)
    }
    fun onBlazeHit(event: EntityDamageByEntityEvent) {
        event.damage += 2.0
    }
    // ----------------------- ENDERMAN -----------------------------
    fun onEndermanInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("ENDERMAN")
        applyEndermanSpeed(player)
    }
    private fun applyEndermanSpeed(player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.14
    }
    fun onEndermanJoin(event: PlayerJoinEvent) = applyEndermanSpeed(event.player)
    fun onEndermanRespawn(event: PlayerRespawnEvent) = applyEndermanSpeed(event.player)
    fun onEndermanRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (plugin.playerMobMap[player]?.equals("ENDERMAN", ignoreCase = true) != true) return

        // Only activate on right-click with an Ender Pearl
        if (!event.action.name.contains("RIGHT_CLICK")) return
        if (player.inventory.itemInMainHand.type != Material.ENDER_PEARL) return

        event.isCancelled = true // Cancel normal throw

        // Find the block the player is looking at within 128 blocks
        val targetBlock = player.getTargetBlockExact(128) ?: run {
            return
        }
        val targetLoc = targetBlock.location.add(0.0, 1.0, 0.0) // teleport above block

        // Safety check: make sure location is safe
        if (targetLoc.block.type.isSolid || targetLoc.add(0.0, 1.0, 0.0).block.type.isSolid) {
            player.sendMessage("${ChatColor.RED}That spot is obstructed!")
            return
        }
        // Teleport player instantly
        player.teleport(targetLoc)
        player.world.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
        player.world.spawnParticle(Particle.PORTAL, targetLoc, 30, 0.5, 1.0, 0.5, 0.2)

        // Optional: cooldown (2 seconds)
        player.setCooldown(Material.ENDER_PEARL, 40)
    }

    // ----------------------- ENDER DRAGON -------------------------
    fun onEnderDragonInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("ENDERDRAGON")
        plugin.enableFlight(player)
        player.flySpeed = 0.3F
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0))
    }
    fun onEnderDragonMove(event: PlayerMoveEvent) {
        val player = event.player

        // Remove negative effects
        listOf(
            PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS,
            PotionEffectType.POISON, PotionEffectType.WITHER,
            PotionEffectType.SLOW_FALLING
        ).forEach { player.removePotionEffect(it) }

        // Heal near Ender Crystals
        val crystals = player.getNearbyEntities(5.0, 5.0, 5.0).filterIsInstance<EnderCrystal>()
        if (crystals.isNotEmpty() && !player.hasPotionEffect(PotionEffectType.REGENERATION)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 60, 1))
        }
        // Track if the player is inside a stronghold
        val loc = player.location
        if (isInStronghold(loc)) {
            val key = strongholdKey(loc)
            rememberStronghold(player, key)
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
     * Store stronghold key in player's persistent data (built-in storage)
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
    /**
     * Get all visited strongholds from player’s data container
     */
    private fun getVisitedStrongholds(player: Player): List<String> {
        val data = player.persistentDataContainer
        val strongholdKey = NamespacedKey(plugin, "visited_strongholds")
        val stored = data.get(strongholdKey, PersistentDataType.STRING) ?: ""
        return stored.split(",").filter { it.isNotBlank() }
    }
    /**
     * Detects if a player is near a stronghold portal
     */
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
    /**
     * Generates a simple stronghold key by grouping chunks
     */
    private fun strongholdKey(loc: Location): String {
        val chunk = loc.chunk
        return "${chunk.world.name}_${chunk.x shr 2}_${chunk.z shr 2}"
    }
    // ----------------------- TUFF GOLEM ---------------------------
    fun onTuffGolemInitialized(player: Player) {
        player.getAttribute(Attribute.ARMOR_TOUGHNESS)?.baseValue =
            (player.getAttribute(Attribute.ARMOR_TOUGHNESS)?.baseValue ?: 0.0) + 1.0
    }
    fun onTuffGolemHit(event: EntityDamageByEntityEvent) {
        val player = event.entity
        if (player is Player && plugin.playerMobMap[player] == "TUFFGOLEM") {
            event.damage *= 0.8 // 20% damage reduction
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
        }
    }
    // ----------------------- GHAST -------------------------------
    fun onGhastInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("GHAST")
        plugin.enableFlight(player)
        player.flySpeed = 0.1F
    }
    fun onGhastFireball(event: PlayerInteractEvent) {
        val player = event.player
        if (plugin.playerMobMap[player] != "GHAST") return
        if (!event.action.name.contains("RIGHT_CLICK")) return
        if (player.inventory.itemInMainHand.type != Material.GHAST_TEAR) return

        val fireball = player.launchProjectile(Fireball::class.java)
        (fireball as org.bukkit.entity.Fireball).setIsIncendiary(true)
        fireball.yield = 1f
        player.world.playSound(player.location, Sound.ENTITY_GHAST_SHOOT, 1f, 1f)
    }
    // ----------------------- AXOLOTL -----------------------------
    fun onAxolotlInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("AXOLOTL")
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 1))
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, Int.MAX_VALUE, 1))
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, Int.MAX_VALUE, 0))
    }

    fun onAxolotlDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (plugin.playerMobMap[player] == "AXOLOTL") {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                player.spigot().respawn()
                player.sendMessage("${ChatColor.AQUA}You instantly respawned as an Axolotl!")
            }, 1L)
        }
    }
    // ----------------------- ELDER GUARDIAN -----------------------
    fun onElderGuardianInitialized(player: Player) {
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
    fun onSkeletonInitialized(player: Player) {
        plugin.mobsToPreventLoot.add("SKELETON")

        // Skeletons are immune to poison
        player.addPotionEffect(PotionEffect(PotionEffectType.POISON, 1, 0, false, false)) // just to clear if present
        player.removePotionEffect(PotionEffectType.POISON)
    }
    fun onSkeletonTick(player: Player) {
        // Continuously remove poison if somehow applied (e.g., splash potions)
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

        // Make arrow "home" towards nearest living entity within 16 blocks (excluding shooter)
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

                // Adjust arrow velocity to slightly curve toward target
                val direction = target.eyeLocation.toVector().subtract(arrow.location.toVector()).normalize()
                arrow.velocity = arrow.velocity.add(direction.multiply(0.2)).normalize().multiply(arrow.velocity.length())
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }
}
