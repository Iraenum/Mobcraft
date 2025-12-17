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
                mobTypes.filter { it.startsWith(args[0].uppercase()) }
            }
            2 -> {
                // Suggest GHAST sub-items if first argument is GHAST
                if (args[0].equals("GHAST", true)) {
                    ghastItems.filter { it.startsWith(args[1].lowercase()) }
                }
                // Suggest potion effects if first argument is ARROW
                else if (args[0].equals("ARROW", true)) {
                    arrowEffects.filter { it.startsWith(args[1].uppercase()) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
