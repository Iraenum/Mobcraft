package org.bench245.mobcraft.command.MobCraft.MobPowers

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class MobPowers(private val plugin: JavaPlugin) : Listener, CommandExecutor, TabCompleter {

    private val blazePlayers = mutableSetOf<String>() // Tracks player names with Blaze powers
    private val supportedMobs = listOf("blaze") // add more mob ids here later

    init {
        // Register as listener and command handler/completer
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.getCommand("setmob")?.setExecutor(this)
        plugin.getCommand("setmob")?.tabCompleter = this
    }

    // Command execution
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.YELLOW}Usage: /setmob <mobType>")
            return true
        }

        val mobType = args[0].lowercase()
        when (mobType) {
            "blaze" -> {
                registerBlaze(sender)
                sender.sendMessage("${ChatColor.GOLD}You have been given Blaze powers! 🔥")
            }
            else -> {
                sender.sendMessage("${ChatColor.RED}Unknown mob type: $mobType")
            }
        }
        return true
    }


    // Tab completion for /setmob
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val completions = mutableListOf<String>()
        if (command.name.equals("setmob", ignoreCase = true)) {
            if (args.size == 1) {
                val partial = args[0].lowercase()
                for (m in supportedMobs) {
                    if (m.startsWith(partial)) completions.add(m)
                }
            }
        }
        return completions
    }

    // Register Blaze powers
    private fun registerBlaze(player: Player) {
        blazePlayers.add(player.name)
        player.sendMessage("${ChatColor.GOLD}You feel fiery power surge through you! (+2 Melee Damage)")
    }

    // Reapply message on join if player was Blaze
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (blazePlayers.contains(player.name)) {

        }
    }

    // Fire a small non-explosive fireball only when right-clicking with a Blaze Rod
    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (!blazePlayers.contains(player.name)) return

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
        if (damager is Player && blazePlayers.contains(damager.name)) {
            event.damage += 2.0
            damager.sendMessage("${ChatColor.RED}🔥 Blaze power adds +2 damage! 🔥")
        }
    }

    // Drop blaze rods when a Blaze player dies
    @EventHandler
    fun onPlayerDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity is Player && blazePlayers.contains(entity.name)) {
            entity.world.dropItemNaturally(entity.location, ItemStack(Material.BLAZE_ROD, 128))
        }
    }
}

