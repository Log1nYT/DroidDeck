package com.droiddeck.launcher.session

import android.content.Context
import android.os.PowerManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PowerProfileTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun defaultIsBalanced() {
        context.getSharedPreferences("session", Context.MODE_PRIVATE).edit().clear().apply()
        assertEquals(PowerProfile.BALANCED, PowerProfile.get(context))
    }

    @Test
    fun ecoForces30Fps() {
        assertEquals(30, PowerProfile.effectiveFpsLimit(0, PowerProfile.ECO))
        assertEquals(30, PowerProfile.effectiveFpsLimit(60, PowerProfile.ECO))
        assertEquals(30, PowerProfile.effectiveFpsLimit(120, PowerProfile.ECO))
        assertEquals(30, PowerProfile.effectiveFpsLimit(30, PowerProfile.ECO))
    }

    @Test
    fun balancedCapsUncappedAt60() {
        assertEquals(60, PowerProfile.effectiveFpsLimit(0, PowerProfile.BALANCED))
        assertEquals(60, PowerProfile.effectiveFpsLimit(60, PowerProfile.BALANCED))
        assertEquals(30, PowerProfile.effectiveFpsLimit(30, PowerProfile.BALANCED))
    }

    @Test
    fun performanceRespectsUser() {
        assertEquals(0, PowerProfile.effectiveFpsLimit(0, PowerProfile.PERFORMANCE))
        assertEquals(120, PowerProfile.effectiveFpsLimit(120, PowerProfile.PERFORMANCE))
    }

    @Test
    fun gpuPinOnlyInPerformance() {
        assertFalse(PowerProfile.allowGpuPin(PowerProfile.ECO))
        assertFalse(PowerProfile.allowGpuPin(PowerProfile.BALANCED))
        assertTrue(PowerProfile.allowGpuPin(PowerProfile.PERFORMANCE))
    }

    @Test
    fun ecoNeverWifiHighPerfOrMangoOrRealtime() {
        assertFalse(PowerProfile.allowWifiHighPerf(PowerProfile.ECO))
        assertTrue(PowerProfile.allowWifiHighPerf(PowerProfile.BALANCED))
        assertFalse(PowerProfile.allowRealtime(PowerProfile.ECO))
        assertFalse(PowerProfile.allowRealtime(PowerProfile.BALANCED))
        assertTrue(PowerProfile.allowRealtime(PowerProfile.PERFORMANCE))
        assertFalse(PowerProfile.effectiveMangoapp(true, PowerProfile.ECO))
        assertTrue(PowerProfile.effectiveMangoapp(true, PowerProfile.BALANCED))
    }

    @Test
    fun ecoEnvOnlyOutsidePerformance() {
        assertTrue(PowerProfile.ecoEnv(PowerProfile.ECO).contains("DXVK_ASYNC=1"))
        assertTrue(PowerProfile.ecoEnv(PowerProfile.BALANCED).contains("MESA_VK_WSI_PRESENT_MODE=fifo"))
        assertTrue(PowerProfile.ecoEnv(PowerProfile.PERFORMANCE).isEmpty())
    }

    @Test
    fun roundTrip() {
        PowerProfile.set(context, PowerProfile.ECO)
        assertEquals(PowerProfile.ECO, PowerProfile.get(context))
        PowerProfile.set(context, "bogus")
        assertEquals(PowerProfile.BALANCED, PowerProfile.get(context))
    }

    @Test
    fun screenBrightnessDefaultsToSystem() {
        context.getSharedPreferences("session", Context.MODE_PRIVATE).edit().clear().apply()
        assertEquals(-1f, SessionPrefs.screenBrightness(context))
    }

    @Test
    fun screenBrightnessRoundTripAndClamp() {
        SessionPrefs.setScreenBrightness(context, 0.42f)
        assertEquals(0.42f, SessionPrefs.screenBrightness(context))
        SessionPrefs.setScreenBrightness(context, 5f)
        assertEquals(1f, SessionPrefs.screenBrightness(context))
    }
}
