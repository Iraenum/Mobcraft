package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class FlightCompleter(private val plugin: Mobcraft) : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        val suggestions = mutableListOf<String>()
        val onlinePlayers = Bukkit.getOnlinePlayers().map { it.name }

        when (args.size) {
            1 -> {
                if (sender.isOp) {
                    suggestions.addAll(listOf("add", "remove", "setSpeed"))
                }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "add" -> suggestions.addAll(onlinePlayers.filter { player -> !plugin.flyingPlayers.contains(player) })
                    "remove" -> suggestions.addAll(plugin.flyingPlayers)
                    else -> suggestions.addAll(onlinePlayers)
                }
            }
        }

        return suggestions
    }
}
