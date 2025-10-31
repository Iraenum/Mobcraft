package org.bench245.mobcraft
import net.kyori.adventure.util.TriState
import org.bench245.mobcraft.command.*
import org.bench245.mobcraft.command.MobCraft.MobPowers.MobPowers
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.loot.LootContext.Builder
import org.bukkit.plugin.java.JavaPlugin

class Mobcraft : JavaPlugin(), Listener, CommandExecutor {
    private var lootEnabled = false // Default state for loot drops
    val mobsToPreventLoot = mutableSetOf<String>() // Set to store mob types
    val flyingPlayers = mutableSetOf<String>()
    val playerMobMap: MutableMap<Player, String> = mutableMapOf()
    val takenMobs = mutableSetOf<String>()
    private val lootToggleCommand = LootToggle(this)
    private val flightCommand = Flight(this)
    private val mobPower = MobPower(this)
    private lateinit var mobPowers: MobPowers


    override fun onEnable() {

        server.pluginManager.registerEvents(this, this)
        getCommand("loottoggle")?.setExecutor(lootToggleCommand)
        getCommand("loottoggle")?.tabCompleter = LootToggleCompleter(this) // Register the tab completer
        getCommand("flight")?.setExecutor(flightCommand)
        getCommand("flight")?.tabCompleter = FlightCompleter(this)
        getCommand("setmob")?.setExecutor(this)
        getCommand("setmob")?.tabCompleter = MobPowerCompleter(this)
        loadMobcraftConfig() // Load the mobs from the config
        logger.info("LootTableControlPlugin has been enabled!")
        fun onEnable() {
            mobPowers = MobPowers(this)
        }

    }

    override fun onDisable() {
        saveMobcraftConfig()
    }

    private fun loadMobcraftConfig() {
        // Load mobs to prevent loot
        val mobsConfig = config.getStringList("mobsToPreventLoot")
        mobsToPreventLoot.clear() // Clear the current list
        mobsToPreventLoot.addAll(mobsConfig) // Load the mobs from the config

        // Load flying players
        val flyingPlayersConfig = config.getStringList("flyingPlayers")
        flyingPlayers.clear() // Clear the current list
        flyingPlayers.addAll(flyingPlayersConfig) // Load the flying players from the config
    }

    private fun saveMobcraftConfig() {
        // Save the current list of mobs to prevent loot
        config.set("mobsToPreventLoot", mobsToPreventLoot.toList())
        // Save the current list of flying players
        config.set("flyingPlayers", flyingPlayers.toList())
        // Save the current list of mob powers
        config.set("payerMobMap", playerMobMap)
        config.set("takenMobs", takenMobs.toList())
        saveConfig() // Save the config file
    }

    fun setPlayerMob(player: Player, mob: String) {
        playerMobMap[player] = mob
    }

    fun getPlayerMob(player: Player) {
        playerMobMap.getOrDefault(player, "none")
    }

    fun enableFlight(player: Player?) {
        if (player!!.name in flyingPlayers) {
            player.allowFlight = true
            player.setFlyingFallDamage(TriState.TRUE)
        }
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
        enableFlight(event.player)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        enableFlight(event.player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val location = player.location
        val damageSource = player.lastDamageCause
        val random = java.util.Random()
        val key = playerMobMap[player] as String
        val contextBuilder = Builder(location)
            .lootedEntity(player)
        enableFlight(player)
        if (damageSource is EntityDamageByEntityEvent) {
            val killer = damageSource.damager // This is the entity that caused the damage
            if (killer is Player) { // Check if the killer is a player
                logger.info("Killer: $killer") // Log the killer
                contextBuilder.killer(killer)
            }
        }
        val context = contextBuilder.build()

        val lootTable = server.getLootTable(NamespacedKey("minecraft", key))
        // Check if the loot table exists
        if (lootTable != null) {
            val loot = lootTable.populateLoot(random, context)
            for (item in loot) {
                location.world.dropItemNaturally(location, item)
            }
        } else {
            logger.warning("Loot table not found: minecraft:$key")
        }

        @EventHandler
        fun onPlayerRightClick(event: PlayerInteractEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "BLAZE" -> mobPowers.onBlazeRightClick(event)
                "ENDERMAN" -> mobPowers.onEndermanRightClick(event)
                "GHAST" -> mobPowers.onGhastFireball(event)
                "ELDER_GUARDIAN" -> mobPowers.onElderGuardianLaser(event)
            }
        }

        @EventHandler
        fun onPlayerHit(event: EntityDamageByEntityEvent) {
            val player = event.entity as? Player ?: return
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "BLAZE" -> mobPowers.onBlazeHit(event)
                "TUFFGOLEM" -> mobPowers.onTuffGolemHit(event)
            }
        }

        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "ENDERMAN" -> mobPowers.onEndermanJoin(event)
            }
        }

        @EventHandler
        fun onPlayerRespawn(event: PlayerRespawnEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "ENDERMAN" -> mobPowers.onEndermanRespawn(event)
                "TUFFGOLEM" -> mobPowers.openTuffGolemEnderChest(player)
            }
        }

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "ENDERDRAGON" -> mobPowers.onEnderDragonMove(event)
                "SKELETON" -> mobPowers.onSkeletonTick(player)
            }
        }

        @EventHandler
        fun onPlayerBreak(event: BlockBreakEvent) {
            val player = event.player
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "ENDERDRAGON" -> mobPowers.onEnderDragonBreak(event)
            }
        }

        @EventHandler
        fun onPlayerShoot(event: EntityShootBowEvent) {
            val entity = event.entity
            if (entity !is Player) return
            val mob = playerMobMap[entity]?.uppercase() ?: return

            when (mob) {
                "SKELETON" -> mobPowers.onSkeletonShoot(event)
            }
        }

        @EventHandler
        fun onPlayerDeath(event: PlayerDeathEvent) {
            val player = event.entity
            val mob = playerMobMap[player]?.uppercase() ?: return

            when (mob) {
                "TUFFGOLEM" -> mobPowers.onTuffGolemDeath(event)
                "AXOLOTL" -> mobPowers.onAxolotlDeath(event)
            }
        }
    }
}