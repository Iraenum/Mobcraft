package org.bench245.mobcraft.data

import org.bench245.mobcraft.Mobcraft
import org.bukkit.scheduler.BukkitRunnable

class TimerTask(private val plugin: Mobcraft, private val manager: PunishmentManager) : BukkitRunnable() {

    override fun run() {
        manager.checkTimers()
    }
}
