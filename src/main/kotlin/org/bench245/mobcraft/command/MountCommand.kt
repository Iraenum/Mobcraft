package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.*

class MountCommand (private val plugin: Mobcraft) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return false
        val mob = plugin.playerMobMap[sender.uniqueId]?.uppercase()
        if (mob != "GHAST") {sender.sendMessage("You must be a ghast to use this command"); return false}
        val player = sender
        val location = player.location

        if (args.isNotEmpty()) {
            val passengers = player.passengers
            if (passengers.isEmpty()) {
                player.sendMessage("You have no entities riding you.")
                return true
            }
            passengers.forEach { player.removePassenger(it) }
            return true
        }
        if (!player.passengers.isEmpty()) return false

        val nearbyEntities = player.getNearbyEntities(2.0, 2.0, 2.0)
            .filter { it != player }
            .filter { it.passengers.isEmpty() }
            .filter { it.location.distance(location) <= 2.0 }
            .filter { it !is Projectile && it !is Item }
            .filter { it.vehicle != player }

        if (nearbyEntities.isEmpty()) {
            player.sendMessage("§cNo entities found within 2 blocks.")
            return true
        }

        val nearest: Entity = nearbyEntities.minByOrNull { it.location.distance(location) }!!
        player.addPassenger(nearest)
        return true
    }
}