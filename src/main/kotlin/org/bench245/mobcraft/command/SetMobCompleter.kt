package org.bench245.mobcraft.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class SetMobCompleter() : TabCompleter {
    private val mobs = listOf("axolotl", "blaze", "ender_dragon", "enderman","ghast","tuffgolem" , "elder_guardian")
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String?>? {
        val suggestions = mutableListOf<String>()
        if (args.size < 2) {
            suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
        } else if (args.size == 2) {
            suggestions.addAll(mobs)
        }
        return suggestions
    }
}