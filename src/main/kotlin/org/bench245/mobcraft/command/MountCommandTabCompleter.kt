package org.bench245.mobcraft.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MountCommandTabCompleter  : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String?>? {
        val suggestions = mutableListOf<String>()
        if (args.size < 2) {
            suggestions.add("dismount")
        }
        return suggestions
    }
}