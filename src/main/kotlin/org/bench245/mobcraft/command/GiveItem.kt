package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GiveItem(private val plugin: Mobcraft) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return true
        val player = sender
        var amount: Int

        val mobType = plugin.playerMobMap[player]?.uppercase() ?: run {
            player.sendMessage("§cYou don't have a mob assigned.")
            return true
        }

        if (args.isEmpty()) {
            player.sendMessage(
                "§cUsage: /giveitem <mobitem> <amount>"
            )
            return true
        }

        if (args.size == 1) {
            amount = 1
        } else {
            amount = args[1].toIntOrNull()?: 1
        }

        when {
            // GHAST-specific items
            mobType == "GHAST" -> {
                val type = args[0].lowercase()
                when (type) {
                    "ghast_tear" -> giveItem(player, Material.GHAST_TEAR, amount)
                    "gunpowder" -> giveItem(player, Material.GUNPOWDER, amount)
                    else -> player.sendMessage("§cInvalid type. Use 'ghast_tear' or 'gunpowder'.")
                }
                return true
            }
            mobType == "BLAZE" -> {
                val type = args[0].lowercase()
                when (type) {
                    "blaze_rod" -> giveItem(player, Material.BLAZE_ROD, amount)
                    else -> player.sendMessage("§cInvalid type. Use 'blaze_rod'.")
                }
                return true
            }
            mobType == "ENDERMAN" -> {
                val type = args[0].lowercase()
                when (type) {
                    "ender_pearl" -> giveItem(player, Material.ENDER_PEARL, amount)
                    else -> player.sendMessage("§cInvalid type. Use 'ender_pearl'.")
                }
                return true
            }
            mobType == "TUFFGOLEM" -> {
                val type = args[0].lowercase()
                when (type) {
                    "tuff" -> giveItem(player, Material.TUFF, amount)
                    else -> player.sendMessage("§cInvalid type. Use 'tuff'.")
                }
                return true
            }
            mobType == "ENDER_DRAGON" -> {
                val type = args[0].lowercase()
                when (type) {
                    "dragon_egg" -> giveItem(player, Material.DRAGON_EGG, amount)
                    "dragon_breath" -> giveItem(player, Material.DRAGON_BREATH, amount)
                    "end_portal_frame" -> giveItem(player, Material.END_PORTAL_FRAME, amount)
                    else -> player.sendMessage("§cInvalid type. Use 'ender_pearl'.")
                }
                return true
            }
        }
        return true
    }
    private fun giveItem(player: Player, material: Material, amount: Int) {
        val item = ItemStack(material, amount)
        player.inventory.addItem(item)
    }

    //private fun giveSkeletonBow(player: Player) {
        //val bow = ItemStack(Material.BOW, 1)
        //val meta = bow.itemMeta
        //meta?.addEnchant(Enchantment.POWER, 8, true)
        //meta?.addEnchant(Enchantment.VANISHING_CURSE, 1, true)
        //bow.itemMeta = meta
        //player.inventory.addItem(bow)
    //}
}
