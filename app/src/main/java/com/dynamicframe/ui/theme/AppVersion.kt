package com.dynamicframe.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dynamicframe.BuildConfig

/** Versión de la app (desde `app/version.properties` → BuildConfig), estilo MakiX. */
object AppVersion {
    fun formatName(raw: String = BuildConfig.VERSION_NAME): String {
        val parts = raw.trim().removePrefix("v").removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }
        return when {
            parts.size >= 3 -> "${parts[0]}.${parts[1]}.${parts[2]}"
            parts.size == 2 -> "${parts[0]}.${parts[1]}.0"
            parts.size == 1 -> "${parts[0]}.0.0"
            else -> raw.ifBlank { "0.0.0" }
        }
    }

    fun shortLabel(): String = "v${formatName()}"

    fun fullLabel(): String = "${shortLabel()} (${BuildConfig.VERSION_CODE})"
}

@Composable
fun AppVersionLabel(
    modifier: Modifier = Modifier,
    showBuildCode: Boolean = false,
    fontSize: TextUnit = 11.sp,
    color: Color = MemoriaMuted
) {
    Text(
        text = if (showBuildCode) AppVersion.fullLabel() else AppVersion.shortLabel(),
        modifier = modifier,
        fontSize = fontSize,
        color = color,
        fontFamily = FontFamily.Monospace
    )
}
