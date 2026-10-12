package com.droiddeck.launcher.session

import kotlin.math.abs

/**
 * Native battery-life forecast feeding Steam's time estimates (`/run/vpower`,
 * what the Quick Access Menu shows as remaining/full-in time), eco-stable fork.
 *
 * The stock component divides the charge counter by the *instantaneous* average
 * current, so the QAM jumps with every load spike - or shows "?h ?m" where the
 * vendor's current API reports nothing (some Xiaomi kernels). This keeps two
 * signals per session and prefers them in order:
 *
 * 1. an EMA of |current| (alpha 0.15 per 5 s sample): spikes settle instead of
 *    swinging the clock; the first sample seeds the average, so a fresh session
 *    reports exactly what the instantaneous math would;
 * 2. the observed %/min rate over a 30 min window (needs >= 3 min and >= 1 %
 *    of movement): works with no current API at all, both discharging and
 *    charging.
 *
 * Pure Kotlin, no Android calls - unit-tested. The guest's raw sysfs
 * (`current_now`, `power_now`) keeps the instantaneous values for MangoHud;
 * only the QAM's forecast seconds are smoothed.
 */
class BatteryForecast {
    private var emaUa: Double? = null
    private val levels = ArrayDeque<Pair<Long, Int>>()
    private var charging = false
    private var lastPct = 50

    fun addSample(nowMs: Long, chargeUah: Long?, currentUa: Long?, pct: Int, charging: Boolean) {
        this.charging = charging
        lastPct = pct
        val a = currentUa?.let { abs(it) }?.takeIf { it > 0 }?.toDouble()
        if (a != null) emaUa = emaUa?.let { it + EMA_ALPHA * (a - it) } ?: a
        levels.addLast(nowMs to pct)
        while (levels.isNotEmpty() && nowMs - levels.first().first > WINDOW_MS) levels.removeFirst()
    }

    /** Minutes until empty while discharging; null while charging or with no signal. */
    fun minutesToEmpty(chargeUah: Long?): Long? {
        if (charging) return null
        val ema = emaUa
        if (ema != null && ema > 0 && chargeUah != null && chargeUah > 0) {
            return ((chargeUah * 60.0 / ema).toLong()).coerceIn(1, MAX_MIN)
        }
        val rate = percentRatePerMin(discharging = true) ?: return null
        return ((lastPct / rate).toLong()).coerceIn(1, MAX_MIN)
    }

    /** Minutes until full while charging; null otherwise or with no signal. */
    fun minutesToFull(chargeFullUah: Long?, chargeUah: Long?): Long? {
        if (!charging) return null
        val ema = emaUa
        if (ema != null && ema > 0 && chargeFullUah != null && chargeUah != null && chargeFullUah > chargeUah) {
            return (((chargeFullUah - chargeUah) * 60.0 / ema).toLong()).coerceIn(1, MAX_MIN)
        }
        val rate = percentRatePerMin(discharging = false) ?: return null
        if (lastPct >= 100) return null
        return (((100 - lastPct) / rate).toLong()).coerceIn(1, MAX_MIN)
    }

    /** %/min from the window's endpoints; null until the movement is real, not noise. */
    private fun percentRatePerMin(discharging: Boolean): Double? {
        if (levels.size < 2) return null
        val first = levels.first()
        val last = levels.last()
        val dtMin = (last.first - first.first) / 60000.0
        if (dtMin < MIN_SPAN_MIN) return null
        val delta = if (discharging) first.second - last.second else last.second - first.second
        if (delta < MIN_DELTA_PCT) return null
        return delta / dtMin
    }

    companion object {
        private const val EMA_ALPHA = 0.15
        private const val WINDOW_MS = 30L * 60 * 1000
        private const val MIN_SPAN_MIN = 3.0
        private const val MIN_DELTA_PCT = 1
        private const val MAX_MIN = 48L * 60
    }
}
