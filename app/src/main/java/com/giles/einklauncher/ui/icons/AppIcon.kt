package com.giles.einklauncher.ui.icons

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import com.giles.einklauncher.data.apps.PinnedAppRef
import com.giles.einklauncher.ui.theme.EinkType
import com.giles.einklauncher.ui.theme.White
import com.giles.einklauncher.ui.theme.einkColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolution the monochrome bitmap is rasterised at; displayed scaled by Compose. */
private const val RENDER_PX = 144

/**
 * Draws a real installed-app icon put through a monochrome filter so it fits the strict
 * black-and-white design: the icon is desaturated to greyscale with a slight contrast boost,
 * and in dark mode its luminance is inverted so it reads as light marks on black. Falls back
 * to a monogram when the icon can't be loaded.
 */
@Composable
fun AppIconImage(
    ref: PinnedAppRef,
    fallback: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = einkColors
    val dark = colors.content == White

    val bitmap by produceState<ImageBitmap?>(initialValue = null, ref, dark) {
        value = withContext(Dispatchers.Default) {
            IconLoader.load(context, ref)?.let { renderMonochrome(it, RENDER_PX, dark) }
        }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        // Fallback while loading or if the icon is unavailable: a monogram in the tile.
        Text(fallback, style = EinkType.Monogram, color = colors.content)
    }
}

/** Loads the launcher icon for a component, falling back to the app icon. */
object IconLoader {
    fun load(context: Context, ref: PinnedAppRef): Drawable? {
        val pm = context.packageManager
        return runCatching {
            pm.getActivityIcon(ComponentName(ref.packageName, ref.activityName))
        }.recoverCatching {
            pm.getApplicationIcon(ref.packageName)
        }.getOrNull()
    }
}

private fun renderMonochrome(source: Drawable, sizePx: Int, dark: Boolean): ImageBitmap {
    val size = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val matrix = ColorMatrix().apply { setSaturation(0f) }   // -> greyscale
    matrix.postConcat(contrastMatrix(1.2f))                  // crisper marks
    if (dark) matrix.postConcat(invertMatrix())              // light-on-dark

    // mutate() so we don't stomp the shared drawable's colour filter.
    val drawable = source.mutate()
    drawable.setBounds(0, 0, size, size)
    drawable.colorFilter = ColorMatrixColorFilter(matrix)
    drawable.draw(canvas)
    drawable.colorFilter = null

    return bitmap.asImageBitmap()
}

private fun contrastMatrix(scale: Float): ColorMatrix {
    val t = (1f - scale) * 128f
    return ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, t,
            0f, scale, 0f, 0f, t,
            0f, 0f, scale, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun invertMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    )
)
