package com.droiddeck.launcher.ui

import android.content.Context
import android.view.KeyEvent
import com.droiddeck.launcher.session.SessionPrefs
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VolumeIndicatorTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun directionMapsVolumeKeys() {
        assertEquals(1, VolumeKeys.direction(KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(-1, VolumeKeys.direction(KeyEvent.KEYCODE_VOLUME_DOWN))
        assertEquals(0, VolumeKeys.direction(KeyEvent.KEYCODE_VOLUME_MUTE))
    }

    @Test
    fun nonVolumeKeysAreNull() {
        assertNull(VolumeKeys.direction(KeyEvent.KEYCODE_BACK))
        assertNull(VolumeKeys.direction(KeyEvent.KEYCODE_BUTTON_A))
        assertNull(VolumeKeys.direction(KeyEvent.KEYCODE_MEDIA_PLAY))
    }

    @Test
    fun inAppVolumeDefaultsOn() {
        context.getSharedPreferences("session", Context.MODE_PRIVATE).edit().clear().apply()
        assertTrue(SessionPrefs.inAppVolume(context))
    }

    @Test
    fun inAppVolumeRoundTrip() {
        SessionPrefs.setInAppVolume(context, false)
        assertFalse(SessionPrefs.inAppVolume(context))
        SessionPrefs.setInAppVolume(context, true)
        assertTrue(SessionPrefs.inAppVolume(context))
    }
}
