package com.droiddeck.launcher.session

import android.os.PowerManager
import org.junit.Assert.*
import org.junit.Test

class ThermalMonitorTest {
    @Test
    fun coolStaysUncapped() {
        assertEquals(0, ThermalMonitor.throttleFps(0, ThermalMonitor.COOL))
        assertEquals(60, ThermalMonitor.throttleFps(60, ThermalMonitor.COOL))
    }

    @Test
    fun stepsAre604030() {
        assertEquals(60, ThermalMonitor.throttleFps(0, ThermalMonitor.WARM))
        assertEquals(40, ThermalMonitor.throttleFps(0, ThermalMonitor.HOT))
        assertEquals(30, ThermalMonitor.throttleFps(0, ThermalMonitor.CRITICAL))
        assertEquals(60, ThermalMonitor.throttleFps(120, ThermalMonitor.WARM))
        assertEquals(40, ThermalMonitor.throttleFps(120, ThermalMonitor.HOT))
        assertEquals(30, ThermalMonitor.throttleFps(120, ThermalMonitor.CRITICAL))
        // Never raises the user's cap.
        assertEquals(30, ThermalMonitor.throttleFps(30, ThermalMonitor.WARM))
    }

    @Test
    fun statusMapping() {
        assertEquals(ThermalMonitor.COOL, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, false, null))
        assertEquals(ThermalMonitor.WARM, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_MODERATE, false, null))
        assertEquals(ThermalMonitor.HOT, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_SEVERE, false, null))
        assertEquals(ThermalMonitor.CRITICAL, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_CRITICAL, false, null))
        assertEquals(ThermalMonitor.CRITICAL, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_SHUTDOWN, false, null))
    }

    @Test
    fun powerSaveForcesWarm() {
        assertEquals(ThermalMonitor.WARM, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, true, null))
    }

    @Test
    fun headroomTrips() {
        assertEquals(ThermalMonitor.CRITICAL, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, false, 0.1f))
        assertEquals(ThermalMonitor.HOT, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, false, 0.3f))
        assertEquals(ThermalMonitor.WARM, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, false, 0.5f))
        assertEquals(ThermalMonitor.COOL, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, false, 0.9f))
        assertEquals(ThermalMonitor.COOL, ThermalMonitor.levelFromSignals(PowerManager.THERMAL_STATUS_NONE, false, Float.NaN))
    }

    @Test
    fun unpinOnlyHotPlus() {
        assertFalse(ThermalMonitor.shouldUnpinGpu(ThermalMonitor.COOL))
        assertFalse(ThermalMonitor.shouldUnpinGpu(ThermalMonitor.WARM))
        assertTrue(ThermalMonitor.shouldUnpinGpu(ThermalMonitor.HOT))
        assertTrue(ThermalMonitor.shouldUnpinGpu(ThermalMonitor.CRITICAL))
    }
}
