package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.ChatColor

class GiveItem(private val plugin: Mobcraft) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return true
        val player = sender

        val mobType = plugin.playerMobMap[player]?.uppercase() ?: run {
            player.sendMessage("§cYou don't have a mob assigned.")
            return true
        }

        if (args.isEmpty()) {
            player.sendMessage(
                "§cUsage: /giveitem <amount> OR /giveitem bow <amount> OR /giveitem arrow <effect> <amount> OR /giveitem ghast <tear|gunpowder> <amount>"
            )
            return true
        }

        val first = args[0].lowercase()

        when {
            // SKELETON-specific bow
            first == "bow" -> {
                if (mobType != "SKELETON") {
                    player.sendMessage("§cOnly skeletons can receive this bow.")
                    return true
                }
                val amount = args.getOrNull(1)?.toIntOrNull() ?: 1
                repeat(amount) { giveSkeletonBow(player) }
                return true
            }

            // SKELETON-specific arrows
            first == "arrow" || first == "arrows" -> {
                if (mobType != "SKELETON") {
                    player.sendMessage("§cOnly skeletons can receive tipped arrows this way.")
                    return true
                }
                if (args.size < 3) {
                    player.sendMessage("§cUsage: /giveitem arrow <EFFECT_NAME> <amount>")
                    return true
                }
                val effectName = args[1].uppercase()
                val amount = args[2].toIntOrNull() ?: 1

                val potionType = try {
                    PotionEffectType.getByName(effectName) ?: throw IllegalArgumentException("Unknown effect")
                } catch (e: Exception) {
                    player.sendMessage("§cUnknown potion effect: $effectName")
                    return true
                }

                giveTippedArrows(player, potionType, amount)
                return true
            }

            // GHAST-specific items
            first == "ghast" -> {
                if (mobType != "GHAST") {
                    player.sendMessage("§cOnly Ghasts can use this command.")
                    return true
                }
                if (args.size < 3) {
                    player.sendMessage("§cUsage: /giveitem ghast <tear|gunpowder> <amount>")
                    return true
                }
                val type = args[1].lowercase()
                val amount = args[2].toIntOrNull() ?: 1

                when (type) {
                    "tear" -> giveItem(player, Material.GHAST_TEAR, amount)
                    "gunpowder" -> giveItem(player, Material.GUNPOWDER, amount)
                    else -> player.sendMessage("§cInvalid type. Use 'tear' or 'gunpowder'.")
                }
                return true
            }

            // Default: give standard mob items by amount
            else -> {
                val amount = first.toIntOrNull() ?: run {
                    player.sendMessage(
                        "§cInvalid amount or command. Use /giveitem <amount> or /giveitem bow <amount> or /giveitem arrow <effect> <amount>"
                    )
                    return true
                }

                when (mobType) {
                    "BLAZE" -> giveItem(player, Material.BLAZE_ROD, amount)
                    "ENDERMAN" -> giveItem(player, Material.ENDER_PEARL, amount)
                    "ELDER_GUARDIAN" -> {
                        giveItem(player, Material.PRISMARINE_SHARD, amount)
                        giveItem(player, Material.SPONGE, amount)
                    }
                    "TUFFGOLEM" -> giveItem(player, Material.TUFF, amount)
                    "AXOLOTL" -> giveItem(player, Material.TROPICAL_FISH_BUCKET, amount)
                    "SKELETON" -> giveItem(player, Material.BONE, amount)
                    "ENDERDRAGON", "ENDER_DRAGON" -> giveItem(player, Material.DRAGON_BREATH, amount)
                    else -> player.sendMessage("§cNo items defined for your mob.")
                }
                return true
            }
        }
    }

    private fun giveItem(player: Player, material: Material, amount: Int) {
        val item = ItemStack(material, amount)
        player.inventory.addItem(item)
        player.sendMessage("§aYou received §e$amount ${material.name}§a!")
    }

    private fun giveSkeletonBow(player: Player) {
        val bow = ItemStack(Material.BOW, 1)
        val meta = bow.itemMeta
        meta?.addEnchant(Enchantment.POWER, 8, true)
        meta?.addEnchant(Enchantment.VANISHING_CURSE, 1, true)
        bow.itemMeta = meta
        player.inventory.addItem(bow)
        player.sendMessage("§aYou received a §ePower 8 Bow§a with Curse of Vanishing!")
    }

    private fun giveTippedArrows(player: Player, effect: PotionEffectType, amount: Int) {
        val arrow = ItemStack(Material.TIPPED_ARROW, amount)
        val meta = arrow.itemMeta as? PotionMeta

        if (meta == null) {
            player.inventory.addItem(arrow)
            player.sendMessage("§cCould not apply potion meta, gave plain arrows.")
            return
        }

        val amplifier = 0
        val duration = if (effect.isInstant) 1 else 20 * 30
        val pe = PotionEffect(effect, duration, amplifier, false, false, false)
        meta.addCustomEffect(pe, true)
        meta.setDisplayName("${ChatColor.GRAY}${effect.name}_ARROW")
        arrow.itemMeta = meta
        player.inventory.addItem(arrow)
        player.sendMessage("§aYou received §e$amount ${effect.name} tipped arrows§a!")
    }
}
