package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class SetMobCompleter(private val plugin: Mobcraft) : TabCompleter {
    private val mobs = listOf("axolotl", "blaze","elder_guardian", "ender_dragon", "enderman","ghast","skeleton","TUFFGOLEM","skeleton")
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String?>? {
        val suggestions = mutableListOf<String>()
        if (args.isEmpty()) {
            suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
        } else if (args.size == 1) {
            suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
        } else if (args.size == 2) {
            suggestions.addAll(mobs.filter { mob -> !plugin.takenMobs.contains(mob) })
        }
        return suggestions
    }
}