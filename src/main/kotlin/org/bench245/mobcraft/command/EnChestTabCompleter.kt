package org.bench245.mobcraft.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class EnChestTabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>? {

        if (args.size == 1) {
            return mutableListOf("") // shows nothing
        }

        return mutableListOf()
    }
}
