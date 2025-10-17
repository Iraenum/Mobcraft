package org.bench245.mobcraft
import net.kyori.adventure.util.TriState
import org.bench245.mobcraft.command.*
import org.bench245.mobcraft.command.MobCraft.MobPowers.MobPowers
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.loot.LootContext.Builder
import org.bukkit.plugin.java.JavaPlugin

class Mobcraft : JavaPlugin(), Listener {
    private var lootEnabled = false // Default state for loot drops
    val mobsToPreventLoot = mutableSetOf<String>() // Set to store mob types
    val flyingPlayers = mutableSetOf<String>()
    val playerMobMap: MutableMap<Player, String> = mutableMapOf()
    private val lootToggleCommand = LootToggle(this)
    private val flightCommand = Flight(this)
    private val mobPower = MobPower(this)

    override fun onEnable() {

        server.pluginManager.registerEvents(this, this)
        getCommand("loottoggle")?.setExecutor(lootToggleCommand)
        getCommand("loottoggle")?.tabCompleter = LootToggleCompleter(this) // Register the tab completer
        getCommand("flight")?.setExecutor(flightCommand)
        getCommand("flight")?.tabCompleter = FlightCompleter(this)
        getCommand("setmob")?.setExecutor(mobPower)
        getCommand("setmob")?.tabCompleter = MobPowerCompleter(this)
        loadMobcraftConfig() // Load the mobs from the config
        logger.info("LootTableControlPlugin has been enabled!")
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
        // Check if loot drops are disabled and if the entity is in the specified group
        if (!lootEnabled && mobsToPreventLoot.contains(event.entity.type.name)) {
            // Clear the drops
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
        //val key = "entities/enderman"
        val contextBuilder = Builder(location)
            .lootedEntity(player)
        enableFlight(player)
        // Check if the damage source is an instance of EntityDamageByEntityEvent
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
    }
}
