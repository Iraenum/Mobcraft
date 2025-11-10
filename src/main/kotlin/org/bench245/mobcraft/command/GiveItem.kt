package org.bench245.mobcraft.command

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class GiveItem : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return true
        }

        val player = sender

        if (args.isEmpty()) {
            return true
        }

        val requestedMob = args[0].uppercase()

        // Handle tipped arrows separately
        if (requestedMob == "ARROW") {
            if (args.size < 3) {
                return true
            }

            val effectName = args[1].uppercase()
            val arrowAmount = args[2].toIntOrNull() ?: 1

            val arrow = ItemStack(Material.TIPPED_ARROW, arrowAmount)
            val meta = arrow.itemMeta as? PotionMeta ?: return true

            val effectType = PotionEffectType.getByName(effectName)
            if (effectType == null) {
                return true
            }

            meta.addCustomEffect(PotionEffect(effectType, 600, 1), true)
            arrow.itemMeta = meta

            player.inventory.addItem(arrow)
            player.sendMessage("§aYou received §e$arrowAmount $effectName arrows§a!")
            return true
        }

        // Handle mob-specific items
        val item: ItemStack? = when (requestedMob) {
            "BLAZE" -> ItemStack(Material.BLAZE_ROD, 64)
            "ENDERMAN" -> ItemStack(Material.ENDER_PEARL, 16)
            "SKELETON" -> {
                val bow = ItemStack(Material.BOW)
                val meta = bow.itemMeta
                meta?.addEnchant(Enchantment.POWER, 8, true)
                meta?.addEnchant(Enchantment.VANISHING_CURSE, 1, true)
                bow.itemMeta = meta
                bow
            }
            "GHAST" -> ItemStack(Material.GHAST_TEAR, 8)
            "GHAST" -> ItemStack(Material.GUNPOWDER, 16)
            "ELDER_GUARDIAN" -> ItemStack(Material.PRISMARINE_SHARD, 16)
            "ELDER_GUARDIAN" -> ItemStack(Material.SPONGE, 1)
            "TUFFGOLEM" -> ItemStack(Material.TUFF, 32)
            "AXOLOTL" -> ItemStack(Material.TROPICAL_FISH_BUCKET, 1)
            "ENDERDRAGON" -> ItemStack(Material.DRAGON_BREATH, 16)
            "ENDERDRAGON" -> ItemStack(Material.DRAGON_HEAD, 1)
            else -> null
        }

        if (item == null) {
            return true
        }

        player.inventory.addItem(item)
        player.sendMessage("§aYou received §e${item.amount} ${requestedMob.replace("_", " ")}§a!")
        return true
    }
}
