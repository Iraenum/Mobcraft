package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class LootToggle(private val plugin: Mobcraft) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (args.isEmpty()) {
            return false
        }

        when (args[0].lowercase()) {

            "enable" -> {
                plugin.setLootEnabled(true)
                sender.sendMessage("Loot drops are now enabled.")
            }

            "disable" -> {
                plugin.setLootEnabled(false)
                sender.sendMessage("Loot drops are now disabled.")
            }

            "add" -> {
                if (args.size < 2) {
                    sender.sendMessage("Usage: /loottoggle add <mobType>")
                    return true
                }

                val mob = args[1].uppercase()
                plugin.mobsToPreventLoot.add(mob)

                sender.sendMessage("$mob has been added to the list of mobs that will not drop loot.")
            }

            "remove" -> {
                if (args.size < 2) {
                    sender.sendMessage("Usage: /loottoggle remove <mobType>")
                    return true
                }

                val mob = args[1].uppercase()
                if (plugin.mobsToPreventLoot.remove(mob)) {
                    sender.sendMessage("$mob has been removed from the list of mobs.")
                } else {
                    sender.sendMessage("$mob is not in the list of mobs.")
                }
            }

            else -> {
                sender.sendMessage("Invalid argument. Usage: /loottoggle <enable|disable|add|remove> [mobType]")
            }
        }

        return true
    }
}
