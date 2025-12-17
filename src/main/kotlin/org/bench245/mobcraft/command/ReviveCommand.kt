package org.bench245.mobcraft.command

import org.bench245.mobcraft.data.PunishmentManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReviveCommand(private val punish: PunishmentManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {

        if (args.isEmpty()) {
            sender.sendMessage("/revive <player>")
            return true
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("Player not found.")
            return true
        }

        punish.unpunish(target)
        sender.sendMessage("${target.name} has been revived.")

        return true
    }
}