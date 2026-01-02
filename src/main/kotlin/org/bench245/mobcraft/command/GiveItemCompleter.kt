package org.bench245.mobcraft.commands

import org.bench245.mobcraft.Mobcraft
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class GiveItemCompleter(private val plugin: Mobcraft) : TabCompleter {

    // List of mob types for the /giveitem command
    private val mobTypes = listOf(
        "blaze",
        "ender_man",
        "skeleton",
        "ghast",
        "elder_guardian",
        "tuffgolem",
        "axolotl",
        "ender_dragon",
        "arrow",
        "bow"
    )

    // List of potion effects for tipped arrows
    private val arrowEffects = listOf(
        "harming",
        "poison",
        "regeneration",
        "speed",
        "slowness",
        "weakness",
        "instant_heal",
        "fire_resistance"
    )

    // GHAST subcommands
    private val ghastItems = listOf("tear", "gunpowder")

    // ENDER_DRAGON subcommands
    private val enderDragonItems = listOf("breath", "portal_frames", "egg")

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {

        if (sender !is Player) return emptyList()

        return when (args.size) {
            1 -> {
                // Suggest mob types, BOW, or ARROW
                mobTypes.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                when {
                    args[0].equals("ghast", true) -> ghastItems.filter { it.startsWith(args[1].lowercase()) }
                    args[0].equals("arrow", true) -> arrowEffects.filter { it.startsWith(args[1].lowercase()) }
                    args[0].equals("ender_dragon", true) || args[0].equals("enderdragon", true) ->
                        enderDragonItems.filter { it.startsWith(args[1].lowercase()) }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
