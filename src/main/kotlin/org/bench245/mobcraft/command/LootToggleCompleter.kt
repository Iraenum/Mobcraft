package org.bench245.mobcraft.command

import org.bench245.mobcraft.Mobcraft
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class LootToggleCompleter(private val plugin: Mobcraft) : TabCompleter {
    private val notAddedMobs = listOf("allay", "armadillo", "axolotl", "bat", "bee", "blaze", "bogged", "breeze", "camel", "cat", "cave_spider", "chicken", "cod", "cow", "creaking", "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "ender_dragon", "enderman", "endermite", "evoker", "fox", "frog", "ghast", "giant", "glow_squid", "goat", "guardian", "happy_ghast", "hoglin", "horse", "husk", "illusioner", "iron_golem", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda", "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager", "polar_bear", "pufferfish", "rabbit", "ravager", "salmon", "sheep", "shulker", "silverfish", "skeleton", "skeleton_horse", "slime", "sniffer", "snow_golem", "spider", "squid", "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager", "vindicator", "wandering_trader", "warden", "witch", "wither", "wither_skeleton", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin") // Predefined mob types

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String>? {
        val suggestions = mutableListOf<String>()

        if (args.isEmpty()) {
            suggestions.add("enable")
            suggestions.add("disable")
            suggestions.add("add")
            suggestions.add("remove")
        } else if (args.size == 1) {
            // Suggest commands based on the first argument
            suggestions.addAll(listOf("enable", "disable", "add", "remove"))
        } else if (args.size == 2 && args[0].lowercase() == "add") {
            // Provide suggestions for mobs to add, excluding already added mobs
            suggestions.addAll(notAddedMobs.filter { mob -> !plugin.mobsToPreventLoot.contains(mob.uppercase()) }.map { "minecraft:${it.lowercase()}" })
        } else if (args.size == 2 && args[0].lowercase() == "remove") {
            // Provide suggestions for mobs to remove
            suggestions.addAll(plugin.mobsToPreventLoot.map { "minecraft:${it.lowercase()}" }) // Suggest mobs that have been added
        }

        return suggestions
    }
}