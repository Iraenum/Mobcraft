package org.bench245.mobcraft.command

import net.kyori.adventure.util.TriState
import org.bench245.mobcraft.Mobcraft
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Flight(private val plugin: Mobcraft) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Command logic goes here
        if (sender is Player) {
            if (args.isEmpty()) {
                plugin.enableFlight(sender)
                return true
            }
            if (sender.isOp) {
                val specifiedPlayer = Bukkit.getPlayer(args[1])
                when (args[0].lowercase()) {
                    "add" -> {
                        plugin.flyingPlayers.add(args[1])
                        plugin.enableFlight(specifiedPlayer)
                        sender.sendMessage("${args[1]} has been added to the list of players with flight permissions")
                    }
                    "remove" -> {
                        specifiedPlayer?.allowFlight = false
                        specifiedPlayer?.isFlying = false
                        plugin.flyingPlayers.remove(args[1])
                        sender.sendMessage("${args[1]} has been removed from the list of players with flight permissions")
                    }
                    "setspeed" -> {
                        if (args[2].toFloat() <= 1) {
                            specifiedPlayer?.flySpeed = args[2].toFloat()
                            sender.sendMessage("${args[1]}'s flight speed has been set to ${args[2]}")
                        } else {sender.sendMessage("Flying speed must not be more than 1")}
                    }
                }
                return true
            }
        }
        return false
    }
}