package org.bench245.mobcraft

import net.kyori.adventure.util.TriState
import org.bench245.mobcraft.command.*
import org.bench245.mobcraft.command.MobCraft.MobPowers.MobPowers
import org.bench245.mobcraft.commands.GiveItemCompleter
import org.bench245.mobcraft.data.PunishmentManager
import org.bench245.mobcraft.data.TimerTask
import org.bench245.mobcraft.listener.DeathListener
import org.bench245.mobcraft.listener.RespawnListener
import org.bukkit.command.CommandExecutor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.*
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext.Builder
import org.bukkit.plugin.java.JavaPlugin
import org.bench245.mobcraft.command.EnChestTabCompleter
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.block.Action
import org.bukkit.event.player.*
import org.bukkit.persistence.PersistentDataType

class Mobcraft : JavaPlugin(), Listener, CommandExecutor {

    private var lootEnabled = false
    val mobsToPreventLoot = mutableSetOf<String>()
    val flyingPlayers = mutableSetOf<String>()
    val playerMobMap: MutableMap<Player, String> = mutableMapOf()
    val takenMobs = mutableSetOf<String>()

    private val lootToggleCommand = LootToggle(this)
    private val flightCommand = Flight(this)
    private val setMob = SetMob(this)
    private val giveItem = GiveItem(this)
    private val DRAGON_EGG_KEY = NamespacedKey(this, "bound_dragon_egg")
    lateinit var punishmentManager: PunishmentManager
        private set
    private lateinit var enChest: EnChestCommand

    lateinit var mobPowers: MobPowers

    override fun onEnable() {

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            mobPowers.updateEnderDragonBeams()
        }, 1L, 1L)

        server.pluginManager.registerEvents(this, this)

        punishmentManager = PunishmentManager(this)
        enChest = EnChestCommand(this)
        mobPowers = MobPowers(this)

        // Register listeners
        server.pluginManager.registerEvents(this, this)
        server.pluginManager.registerEvents(DeathListener(this, punishmentManager), this)
        server.pluginManager.registerEvents(RespawnListener(punishmentManager), this)

        getCommand("loottoggle")?.apply {
            setExecutor(lootToggleCommand)
            tabCompleter = LootToggleCompleter(this@Mobcraft)
        }

        getCommand("flight")?.apply {
            setExecutor(flightCommand)
            tabCompleter = FlightCompleter(this@Mobcraft)
        }

        getCommand("giveitem")?.apply {
            setExecutor(giveItem)
            tabCompleter = GiveItemCompleter(this@Mobcraft)
        }

        getCommand("setmob")?.apply {
            setExecutor(setMob)
            tabCompleter = SetMobCompleter(this@Mobcraft)
        }

        val reviveCommand = ReviveCommand(punishmentManager)
        getCommand("revive")?.apply {
            setExecutor(reviveCommand)
            tabCompleter = ReviveCommandCompleter(punishmentManager)
        }

        getCommand("enchest")?.apply {
            setExecutor(enChest)
            tabCompleter = EnChestTabCompleter()
        }


        // Start punishment timer
        TimerTask(this, punishmentManager).runTaskTimer(this, 20L, 1200L)

        loadMobcraftConfig()
        logger.info("MobCraft has been enabled!")
    }

    override fun onDisable() {
        saveMobcraftConfig()
    }

    private fun loadMobcraftConfig() {
        mobsToPreventLoot.clear()
        mobsToPreventLoot.addAll(config.getStringList("mobsToPreventLoot"))

        flyingPlayers.clear()
        flyingPlayers.addAll(config.getStringList("flyingPlayers"))
    }

    private fun saveMobcraftConfig() {
        config.set("mobsToPreventLoot", mobsToPreventLoot.toList())
        config.set("flyingPlayers", flyingPlayers.toList())
        saveConfig()
    }

    fun initializeMob(player: Player, mob: String) {

        mobPowers.resetPlayerState(player)
        when (mob.uppercase()) {
            "ENDERMAN" -> mobPowers.onEndermanInitialize(player)
            "BLAZE" -> mobPowers.onBlazeInitialize(player)
            "ENDER_DRAGON" -> mobPowers.onEnderDragonInitialize(player)
            "GHAST" -> mobPowers.onGhastInitialize(player)
            "TUFFGOLEM" -> mobPowers.onTuffGolemInitialize(player)
            "AXOLOTL" -> mobPowers.onAxolotlInitialize(player)
        }
    }

    fun setPlayerMob(player: Player, mob: String) {
        playerMobMap[player] = mob
    }

    fun resetPlayerState(player: Player) {
        for (effect in player.activePotionEffects) {
            player.removePotionEffect(effect.type)
        }
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.1
        player.getAttribute(Attribute.ARMOR_TOUGHNESS)?.baseValue = 0.0
        player.getAttribute(Attribute.ARMOR)?.baseValue = 0.0
        player.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = 1.0
        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0

        player.health = player.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 20.0

        player.allowFlight = false
        player.isFlying = false
        player.flySpeed = 0.1f
        this.flyingPlayers.remove(player.name)

        player.fireTicks = 0
        player.remainingAir = player.maximumAir

        player.fallDistance = 0f
        player.velocity = player.velocity.zero()

        player.world.entities
            .filterIsInstance<EnderCrystal>()
            .forEach { crystal ->
                if (crystal.beamTarget != null) {
                    crystal.beamTarget = null
                }
            }
    }

    fun getPlayerMob(player: Player): String {
        return playerMobMap.getOrDefault(player, "none")
    }

    fun enableFlight(player: Player?) {
        if (player == null) return
        player.allowFlight = true
        player.isFlying = true
        player.setFlyingFallDamage(TriState.TRUE)
        flyingPlayers.add(player.name)
    }

    fun setLootEnabled(enabled: Boolean) {
        lootEnabled = enabled
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!lootEnabled && mobsToPreventLoot.contains(event.entity.type.name)) {
            event.drops.clear()
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val mob = playerMobMap[player]?.uppercase() ?: return

        Bukkit.getScheduler().runTaskLater(this, Runnable {

            // 🔓 Override punishment for specific mobs
            if (mob == "TUFFGOLEM" || mob == "AXOLOTL") {
                punishmentManager.unpunish(player)
                resetPlayerState(player)
            }

            // ✈ Flight-enabled mobs
            if (mob in listOf("BLAZE", "GHAST", "ENDER_DRAGON")) {
                enableFlight(player)
            }

            // 🧬 Apply mob-specific respawn effects
            when (mob) {
                "BLAZE" -> mobPowers.applyBlazeEffects(player)

                "ENDERMAN" -> mobPowers.onEndermanRespawn(event)

                "ENDER_DRAGON" -> mobPowers.applyDragonEffects(player)

                "TUFFGOLEM" -> mobPowers.applyGolemEffects(player)

                "GHAST" -> mobPowers.applyGhastEffects(player)

                "AXOLOTL" -> mobPowers.applyAxolotlEffects(player)
            }

        }, 1L)
    }
    @EventHandler
    fun onGameModeChange(event: PlayerGameModeChangeEvent) {
        val player = event.player

        mobPowers.resetPlayerState(player)

        val mob = playerMobMap[player]?.uppercase() ?: return

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            when (player.gameMode) {
                org.bukkit.GameMode.SPECTATOR -> {
                    enableFlight(player)
                    player.flySpeed = 0.1f
                }

                else -> {
                    when (mob) {
                        "BLAZE" -> {
                            enableFlight(player)
                            player.flySpeed = 0.03f
                            mobPowers.onBlazeGamemodeChange(event)
                            mobPowers.applyBlazeEffects(player)
                        }

                        "GHAST" -> {
                            enableFlight(player)
                            player.flySpeed = 0.08f
                            mobPowers.applyGhastEffects(player)
                        }

                        "ENDER_DRAGON" -> {
                            enableFlight(player)
                            player.flySpeed = 0.1f
                            mobPowers.applyDragonEffects(player)
                        }

                        "ENDERMAN" -> mobPowers.applyEndermanSpeed(player)

                        "TUFFGOLEM" -> {
                            mobPowers.applyGolemEffects(player)
                        }

                        "AXOLOTL" -> {
                            mobPowers.applyAxolotlEffects(player)
                        }
                    }
                }
            }
        }, 1L)
    }
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        enableFlight(event.player)
    }
    @EventHandler
    fun onEntityExplode(event: ExplosionPrimeEvent) {
        mobPowers.onEnderDragonExplosion(event)
    }
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val location = player.location
        val damageSource = player.lastDamageCause
        val random = java.util.Random()

        mobPowers.onDragonEggDeath(event)

        val key = playerMobMap[player] ?: "none"
        val contextBuilder = Builder(location).lootedEntity(player)
        enableFlight(player)

        if (damageSource is EntityDamageByEntityEvent) {
            val killer = damageSource.damager
            if (killer is Player) {
                logger.info("Killer: $killer")
                contextBuilder.killer(killer)

                // lightning effect
                val world = player.world
                world.strikeLightning(player.location)

                Bukkit.getOnlinePlayers().forEach { p ->
                    p.playSound(
                        player.location,
                        org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                        1.5f,
                        1.0f
                    )
                }
            }
        }

        val context = contextBuilder.build()
        val lootTable = server.getLootTable(NamespacedKey("minecraft", key))

        lootTable?.populateLoot(random, context)?.forEach {
            location.world.dropItemNaturally(location, it)
        }

        val mob = playerMobMap[player]?.uppercase() ?: return
        when (mob) {
            "TUFFGOLEM" -> {
                mobPowers.onTuffGolemDeath(event)
                player.world.dropItemNaturally(location, ItemStack(Material.TUFF, 64))
            }

            "SKELETON" -> player.world.dropItemNaturally(location, ItemStack(Material.BONE, 64))
            "ENDERMAN" -> repeat(4) {
                player.world.dropItemNaturally(location, ItemStack(Material.ENDER_PEARL, 64))
            }

            "ENDER_DRAGON" -> player.world.dropItemNaturally(location, ItemStack(Material.DRAGON_BREATH, 64))
            "BLAZE" -> repeat(2) {
                player.world.dropItemNaturally(location, ItemStack(Material.BLAZE_ROD, 64))
            }

            "GHAST" -> {
                repeat(2) {
                    player.world.dropItemNaturally(location, ItemStack(Material.GHAST_TEAR, 64))
                }
                player.world.dropItemNaturally(location, ItemStack(Material.GUNPOWDER, 64))
            }

            "GUARDIAN", "ELDER_GUARDIAN" -> {
                player.world.dropItemNaturally(location, ItemStack(Material.SPONGE, 64))
                player.world.dropItemNaturally(location, ItemStack(Material.PRISMARINE, 64))
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val mob = playerMobMap[player]?.uppercase() ?: return

        when (mob) {
            "BLAZE" -> mobPowers.onBlazeRightClick(event)

            "GHAST" -> mobPowers.onGhastFireball(event)

            "ENDER_DRAGON" -> mobPowers.onEnderDragonRightClick(event)

            "ENDERMAN" -> mobPowers.onEndermanRightClick(event)
            }
        }
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity

        if (projectile is DragonFireball) {
            mobPowers.onDragonFireballHit(event)
        }
    }
    @EventHandler
    fun onEnderDragonPotionEffect(event: EntityPotionEffectEvent) {
        mobPowers.onEnderDragonPotionEffect(event)
    }
    @EventHandler
    fun onDragonEggDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        val meta = item.itemMeta ?: return
        mobPowers.onDragonEggDrop(event)

        if (meta.persistentDataContainer.has(DRAGON_EGG_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            event.isCancelled = true
            event.player.sendMessage("§cYou cannot drop a Dragon Egg.")
        }

    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        mobPowers.onDragonEggPickup(event)
    }


        @EventHandler
        fun onPlayerDamage(event: EntityDamageByEntityEvent) {
            val player = event.entity as? Player ?: return
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {

                "BLAZE" -> {
                    // Prevent Blaze hurting Blaze-player
                    if (event.damager is org.bukkit.entity.Blaze) {
                        event.isCancelled = true
                    }

                    // 🔥 Call Blaze projectile handler
                    mobPowers.onBlazeProjectileHit(event)
                }

                "GHAST" -> {
                    if (event.damager is org.bukkit.entity.Ghast) event.isCancelled = true
                    mobPowers.onGhastFireballHit(event)
                }

                "SKELETON" -> {
                    if (event.damager is org.bukkit.entity.Skeleton) event.isCancelled = true
                }

                "TUFFGOLEM" -> {
                    mobPowers.onTuffGolemHit(event)
                }

                "ENDERMAN" -> {
                    if (event.damager is org.bukkit.entity.Enderman)
                        event.isCancelled = true

                    if (event.damager is org.bukkit.entity.Projectile) {
                        event.isCancelled = true
                        event.damager.remove()

                        player.world.spawnParticle(
                            org.bukkit.Particle.CRIT,
                            player.location.add(0.0, 1.0, 0.0),
                            10,
                            0.2,
                            0.2,
                            0.2,
                            0.05
                        )
                        player.world.playSound(
                            player.location,
                            org.bukkit.Sound.ENTITY_ITEM_BREAK,
                            1f,
                            1f
                        )
                    }

                    mobPowers.onEndermanProjectileDamage(event)
                }

                "ENDER_DRAGON" -> {
                    if (event.damager is org.bukkit.entity.EnderDragon)
                        event.isCancelled = true
                }

                "ELDER_GUARDIAN" -> {
                    if (event.damager is org.bukkit.entity.ElderGuardian)
                        event.isCancelled = true
                }
            }
        }


        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "BLAZE" -> mobPowers.onBlazeMove(event)
            }
        }

        @EventHandler
        fun onDragonEggPickup(event: PlayerPickupItemEvent) {
            val player = event.player
            val item = event.item.itemStack

            // Delegate to MobPowers
            mobPowers.onDragonEggDrop(return)
        }

        @EventHandler
        fun onPlayerFall(event: EntityDamageEvent) {
            val player = event.entity
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "ENDER_DRAGON" -> mobPowers.onDragonFallDamage(event)
            }
        }

        @EventHandler
        fun onEndermanProjectileDamage(event: EntityDamageByEntityEvent) {
            val target = event.entity as? Player ?: return
            val mob = playerMobMap[target]?.uppercase() ?: return
            if (mob != "ENDERMAN") return

            val projectile = event.damager as? Projectile ?: return

            if (projectile !is Arrow) return

            if (mobPowers.canPierceEndermanShield(projectile.shooter)) return

            val key = NamespacedKey(this, "enderman_reflected")
            if (projectile.persistentDataContainer.has(key, PersistentDataType.BYTE)) return

            event.isCancelled = true

            projectile.persistentDataContainer.set(key, PersistentDataType.BYTE, 1)

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

        @EventHandler
        fun onEntityTarget(event: EntityTargetEvent) {
            val target = event.target
            val entity = event.entity
            if (target !is Player) return
            val playerMob = playerMobMap[target]?.uppercase() ?: return

            if (entity.type.name == playerMob) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
            val damaged = event.entity
            val damager = event.damager
            if (damaged !is Player) return
            val playerMob = playerMobMap[damaged]?.uppercase() ?: return

            if (damager is LivingEntity && damager !is Player) {
                if (damager.type.name == playerMob) {
                    event.isCancelled = true
                    return
                }
            }

            if (damager is Projectile) {
                val shooter = damager.shooter
                if (shooter is LivingEntity && shooter !is Player && shooter.type.name == playerMob) {
                    event.isCancelled = true
                }
            }
        }

        @EventHandler
        fun onPlayerBreak(event: BlockBreakEvent) {

        }

        @EventHandler
        fun onPlayerExplosionDamage(event: EntityDamageEvent) {
            val entity = event.entity as? Player ?: return
            val mob = playerMobMap[entity]?.uppercase() ?: return

            if (mob == "GHAST") mobPowers.onGhastFireball(return)
        }

        @EventHandler
        fun onPlayerMobJoin(event: PlayerJoinEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            if (mob == "ENDERMAN") mobPowers.onEndermanJoin(event)

            server.pluginManager.registerEvents(object : Listener {
                @EventHandler
                fun onProjectileHit(event: ProjectileHitEvent) {
                    mobPowers.onEndermanProjectileDamage(return)
                }
            }, this)
        }
    }
