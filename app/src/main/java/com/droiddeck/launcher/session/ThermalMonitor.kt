package com.droiddeck.launcher.session

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Thermal governor for the eco-stable fork.
 *
 * Reads Android's own thermal signals (no root, no polling of sysfs from here):
 * - PowerManager.currentThermalStatus (API 29+),
 * - PowerManager.isPowerSaveMode,
 * - PowerManager.getThermalHeadroom (API 30+, NaN/negative = unavailable).
 *
 * Level: 0 COOL, 1 WARM, 2 HOT, 3 CRITICAL.
 * The session reacts by capping fps (60/40/30 - all divide a 120 Hz panel evenly)
 * and by dropping the GPU top-clock pin at HOT+. The kernel's own thermal limits
 * always stay on top; this only asks for less *before* the kernel must throttle.
 */
object ThermalMonitor {
    private const val TAG = "ThermalMonitor"

    const val COOL = 0
    const val WARM = 1
    const val HOT = 2
    const val CRITICAL = 3

    @Volatile
    private var running = false
    private var thread: Thread? = null

    /** Pure mapping, unit-tested without Android. */
    fun levelFromSignals(thermalStatus: Int, powerSave: Boolean, headroom: Float?): Int {
        var level = when {
            thermalStatus >= PowerManager.THERMAL_STATUS_EMERGENCY ||
                thermalStatus >= PowerManager.THERMAL_STATUS_SHUTDOWN ||
                thermalStatus >= PowerManager.THERMAL_STATUS_CRITICAL -> CRITICAL
            thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE -> HOT
            thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> WARM
            else -> COOL
        }
        if (powerSave) level = maxOf(level, WARM)
        if (headroom != null && !headroom.isNaN() && headroom >= 0f) {
            level = when {
                headroom < 0.2f -> maxOf(level, CRITICAL)
                headroom < 0.4f -> maxOf(level, HOT)
                headroom < 0.6f -> maxOf(level, WARM)
                else -> level
            }
        }
        return level
    }

    /** Pure fps throttle, unit-tested. 0 = uncapped input. */
    fun throttleFps(requested: Int, level: Int): Int {
        if (level <= COOL) return requested.coerceAtLeast(0)
        val cap = when (level) {
            WARM -> 60
            HOT -> 40
            else -> 30
        }
        return if (requested <= 0) cap else minOf(requested, cap)
    }

    fun shouldUnpinGpu(level: Int): Boolean = level >= HOT

    /** Current level from the live device; never throws. */
    fun currentLevel(context: Context): Int = try {
        val power = context.getSystemService(PowerManager::class.java) ?: return COOL
        val status = if (Build.VERSION.SDK_INT >= 29) power.currentThermalStatus else PowerManager.THERMAL_STATUS_NONE
        val headroom: Float? = if (Build.VERSION.SDK_INT >= 30) {
            try {
                power.getThermalHeadroom(10).takeIf { !it.isNaN() && it >= 0f }
            } catch (_: Throwable) { null }
        } else null
        levelFromSignals(status, power.isPowerSaveMode, headroom)
    } catch (_: Throwable) { COOL }

    /**
     * Starts a 10 s sampler for the running session. On HOT+ it drops the GPU pin
     * (the kernel would throttle anyway; dropping early saves the hitch + heat).
     * It never re-pins: re-pinning mid-game causes a clock jump. The next session
     * start re-evaluates from scratch.
     */
    @Synchronized
    fun start(context: Context) {
        if (running) return
        running = true
        val appCtx = context.applicationContext
        thread = Thread({
            var lastLevel = COOL
            while (running) {
                try { Thread.sleep(30_000L) } catch (_: InterruptedException) { break }
                if (!running) break
                val level = currentLevel(appCtx)
                if (level != lastLevel) {
                    Log.i(TAG, "thermal level $lastLevel -> $level")
                    lastLevel = level
                }
                if (shouldUnpinGpu(level) && GpuClockPin.isPinned) {
                    Log.i(TAG, "thermal level $level: releasing GPU top-clock pin")
                    try { GpuClockPin.unpinForThermal(appCtx) } catch (t: Throwable) {
                        Log.w(TAG, "thermal unpin", t)
                    }
                }
            }
        }, "thermal-monitor").apply { isDaemon = true; start() }
        Log.i(TAG, "thermal monitor started")
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }
}
