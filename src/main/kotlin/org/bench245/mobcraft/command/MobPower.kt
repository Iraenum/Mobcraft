package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class MobPower(private val plugin: Mobcraft) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender.isOp) {
            plugin.setPlayerMob(Bukkit.getPlayer(args[0])!!,args[1])
            plugin.takenMobs.add(args[1])
            return true
        }
        return false
    }
}