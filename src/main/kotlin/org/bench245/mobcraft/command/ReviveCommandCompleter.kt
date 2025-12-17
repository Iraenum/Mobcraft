package org.bench245.mobcraft.command

import org.bench245.mobcraft.data.PunishmentManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class ReviveCommandCompleter(private val punishmentManager: PunishmentManager) : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (args.isEmpty() || args.size == 1) {
            suggestions.addAll(
                punishmentManager
                    .getAllPunishedPlayers()
                    .filter { it.startsWith(args.getOrNull(0) ?: "", ignoreCase = true) }
            )
        }

        return suggestions
    }
}
