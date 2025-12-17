package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class SetMob(private val plugin: Mobcraft) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender.isOp) {
            val mob = args[1].uppercase()
            val player = Bukkit.getPlayer(args[0])!!
            plugin.initializeMob(player,mob)

            plugin.setPlayerMob(player, args[1])
            plugin.takenMobs.add(args[1])
            return true
        }
        return false
    }
}