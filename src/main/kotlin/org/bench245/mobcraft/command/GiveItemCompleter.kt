package org.bench245.mobcraft.commands

import org.bench245.mobcraft.Mobcraft
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class GiveItemCompleter(private val plugin: Mobcraft) : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {

        if (sender !is Player) return emptyList()

        val mobType = plugin.playerMobMap[sender]?.uppercase() ?: run {
            return emptyList()
        }
        return when (args.size) {
            1 -> {
                when (mobType) {
                    "GHAST" -> {
                        listOf(
                            "ghast_tear",
                            "gunpowder"
                        )
                    }
                    "BLAZE" -> {
                        listOf(
                            "blaze_rod"
                        )
                    }
                    "ENDERMAN" -> {
                        listOf(
                            "ender_pearl"
                        )
                    }
                    "TUFFGOLEM" -> {
                        listOf(
                            "tuff"
                        )
                    }
                    "ENDER_DRAGON" -> {
                        listOf(
                            "dragon_egg",
                            "dragon_breath",
                            "end_portal_frame"
                        )
                    }
                    "AXOLOTL" -> {
                        listOf(
                            "you get nothing, idiot"
                        )
                    }

                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}
