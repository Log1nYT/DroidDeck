package com.droiddeck.launcher.session

import org.junit.Assert.*
import org.junit.Test

class BatteryForecastTest {
    @Test
    fun firstSampleMatchesInstantaneousMath() {
        val f = BatteryForecast()
        // 4 Ah at a 2 A draw = 120 min, exactly what the old code wrote on first write.
        f.addSample(0L, 4_000_000L, 2_000_000L, 50, false)
        assertEquals(120L, f.minutesToEmpty(4_000_000L))
        assertNull(f.minutesToFull(8_000_000L, 4_000_000L))
    }

    @Test
    fun emaSmoothsASpikeInsteadOfSwinging() {
        val f = BatteryForecast()
        f.addSample(0L, 4_000_000L, 2_000_000L, 50, false)
        // A 10 A spike: instantaneous would say 24 min, the EMA stays far above it.
        f.addSample(5_000L, 4_000_000L, 10_000_000L, 50, false)
        val smoothed = f.minutesToEmpty(4_000_000L)!!
        assertTrue("smoothed=$smoothed", smoothed > 24L && smoothed < 120L)
    }

    @Test
    fun percentRateCoversADeadCurrentApi() {
        val f = BatteryForecast()
        val min = 60_000L
        // No current at all, 50 -> 48 % over 10 min = 0.2 %/min -> 48/0.2 = 240 min left.
        f.addSample(0L, null, null, 50, false)
        f.addSample(10 * min, null, null, 48, false)
        assertEquals(240L, f.minutesToEmpty(null))
    }

    @Test
    fun percentRatePredictsTimeToFullWhileCharging() {
        val f = BatteryForecast()
        val min = 60_000L
        // 50 -> 55 % over 10 min = 0.5 %/min -> (100-55)/0.5 = 90 min to full.
        f.addSample(0L, null, null, 50, true)
        f.addSample(10 * min, null, null, 55, true)
        assertNull(f.minutesToEmpty(null))
        assertEquals(90L, f.minutesToFull(null, null))
    }

    @Test
    fun noiseAndSingleSamplesGiveNull() {
        val f = BatteryForecast()
        f.addSample(0L, null, null, 50, false)
        assertNull(f.minutesToEmpty(null))
        // Same level for 10 min: no movement, no forecast.
        f.addSample(10 * 60_000L, null, null, 50, false)
        assertNull(f.minutesToEmpty(null))
    }

    @Test
    fun chargingFullMathUsesCurrentFirst() {
        val f = BatteryForecast()
        // (8-4) Ah at 2 A = 120 min to full.
        f.addSample(0L, 4_000_000L, 2_000_000L, 50, true)
        assertEquals(120L, f.minutesToFull(8_000_000L, 4_000_000L))
        assertNull(f.minutesToEmpty(4_000_000L))
    }
}
