package com.droiddeck.launcher.session

import android.content.Context

/**
 * Eco-stable power profiles (fork addition).
 *
 * ECO saves battery/heat at the cost of fps, BALANCED is the default,
 * PERFORMANCE keeps the upstream behaviour (uncapped fps, GPU pin if asked).
 *
 * The mapping is deliberately conservative: it only *caps* what the user left
 * uncapped and only *disables* power-hungry switches outside PERFORMANCE.
 * User choices inside PERFORMANCE are always honoured.
 */
object PowerProfile {
    const val ECO = "eco"
    const val BALANCED = "balanced"
    const val PERFORMANCE = "performance"

    private const val KEY = "powerProfile"

    fun get(context: Context): String =
        context.getSharedPreferences("session", Context.MODE_PRIVATE)
            .getString(KEY, BALANCED)?.takeIf { it in setOf(ECO, BALANCED, PERFORMANCE) }
            ?: BALANCED

    fun set(context: Context, profile: String) {
        val v = profile.takeIf { it in setOf(ECO, BALANCED, PERFORMANCE) } ?: BALANCED
        context.getSharedPreferences("session", Context.MODE_PRIVATE).edit().putString(KEY, v).apply()
    }

    /**
     * Effective fps cap: never raises the user's cap, only lowers uncapped/hot ones.
     * 0 = uncapped (upstream default). ECO forces 30, BALANCED caps uncapped at 60,
     * PERFORMANCE respects the user entirely.
     */
    fun effectiveFpsLimit(userFps: Int, profile: String): Int = when (profile) {
        ECO -> if (userFps <= 0 || userFps > 30) 30 else userFps
        BALANCED -> if (userFps <= 0) 60 else userFps
        else -> userFps.coerceAtLeast(0)
    }

    /** GPU top-clock pin is only honoured in PERFORMANCE (and then only if the user opted in). */
    fun allowGpuPin(profile: String): Boolean = profile == PERFORMANCE

    /** WiFi high-perf lock: ECO never (radio stays in power-save), else defer to DeviceSupport. */
    fun allowWifiHighPerf(profile: String): Boolean = profile != ECO

    /** gamescope realtime queues: PERFORMANCE only. */
    fun allowRealtime(profile: String): Boolean = profile == PERFORMANCE

    /** MangoApp overlay: ECO forces it off (it costs a present + polling threads). */
    fun effectiveMangoapp(userOn: Boolean, profile: String): Boolean =
        if (profile == ECO) false else userOn

    /**
     * Guest env overrides for ECO/BALANCED. All are hints the guest honours if present:
     * - DXVK_ASYNC=1: async shader compile, fewer hitches (no extra power);
     * - VKD3D_CONFIG=no_upload_hvv: avoid host-visible upload heap on Adreno (power + stability);
     * - MESA_VK_WSI_PRESENT_MODE=fifo: vsync present, no busy mailbox loop.
     */
    fun ecoEnv(profile: String): List<String> = when (profile) {
        ECO, BALANCED -> listOf(
            "DXVK_ASYNC=1",
            "VKD3D_CONFIG=no_upload_hvv",
            "MESA_VK_WSI_PRESENT_MODE=fifo",
        )
        else -> emptyList()
    }
}
