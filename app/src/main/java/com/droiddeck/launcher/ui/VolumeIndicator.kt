package com.droiddeck.launcher.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Eco-stable fork: the in-session volume indicator.
 *
 * The system volume panel is a separate window above the compositor's SurfaceView and its
 * appearance tears a frame on Adreno devices. This draws inside the session's own Compose
 * layer instead: no new window, no recomposition glitch. Passive - no clickables, so
 * touches fall through to the game exactly as the overlay documents.
 */
@Composable
fun VolumeIndicator(level: Int, max: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .padding(bottom = 72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val fraction = if (max > 0) level.toFloat() / max else 0f
            Icon(
                when {
                    level <= 0 -> Icons.Filled.VolumeMute
                    fraction < 0.5f -> Icons.Filled.VolumeDown
                    else -> Icons.Filled.VolumeUp
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.width(160.dp),
            )
        }
    }
}

/** Pure key mapping, unit-tested: AudioManager direction or null when the key is not volume. */
object VolumeKeys {
    /** -1 lower, +1 raise, 0 mute-toggle, null not a volume key. */
    fun direction(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> 1
        KeyEvent.KEYCODE_VOLUME_DOWN -> -1
        KeyEvent.KEYCODE_VOLUME_MUTE -> 0
        else -> null
    }
}
