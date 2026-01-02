package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EnChestCommand(private val plugin: Mobcraft) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }

        val player = sender

        val mob = plugin.playerMobMap[player.uniqueId]?.uppercase()
        if (mob != "TUFFGOLEM") {
            player.sendMessage("§cOnly Tuff Golems can open their ender chest.")
            return true
        }
        plugin.mobPowers.openTuffGolemEnderChest(player)
        return true
    }
}
