package com.dynamicframe.presentation.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.ui.theme.MemoriaInk
import com.dynamicframe.ui.theme.MemoriaLine
import com.dynamicframe.ui.theme.MemoriaMuted
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.MemoriaPurpleSoft
import com.dynamicframe.ui.theme.MemoriaSurface
import com.dynamicframe.presentation.device.LocalDeviceProfile

enum class FeatureStatus { IMPLEMENTED, PARTIAL, COMING_SOON }

data class FeatureItem(
    val category: String,
    val name: String,
    val status: FeatureStatus,
    val note: String
)

val memoriaFeatureCatalog: List<FeatureItem> = listOf(
    // Reproducción
    FeatureItem("Reproducción", "Duración por foto (3–120 s)", FeatureStatus.IMPLEMENTED, "Ajustes e intervalo en dashboard"),
    FeatureItem("Reproducción", "Transiciones (fundido, Ken Burns, deslizar…)", FeatureStatus.IMPLEMENTED, "20+ tipos disponibles"),
    FeatureItem("Reproducción", "Orden aleatorio", FeatureStatus.IMPLEMENTED, "Toggle en dashboard"),
    FeatureItem("Reproducción", "Bucle continuo", FeatureStatus.IMPLEMENTED, "Toggle en dashboard"),
    FeatureItem("Reproducción", "Mezcla fotos + vídeos", FeatureStatus.IMPLEMENTED, "Filtro de contenido"),
    FeatureItem("Reproducción", "Velocidad de transición", FeatureStatus.IMPLEMENTED, "Duración en ms en ajustes"),
    FeatureItem("Reproducción", "Transición glitch / cortina", FeatureStatus.COMING_SOON, "Próxima versión"),
  // Contenido
    FeatureItem("Contenido", "Galería local / carpetas SAF", FeatureStatus.IMPLEMENTED, "USB y almacenamiento interno"),
    FeatureItem("Contenido", "Álbumes múltiples", FeatureStatus.IMPLEMENTED, "Píldoras y selección"),
    FeatureItem("Contenido", "Google Photos", FeatureStatus.COMING_SOON, "Requiere API OAuth"),
    FeatureItem("Contenido", "iCloud / Dropbox", FeatureStatus.COMING_SOON, "Integración en nube"),
    FeatureItem("Contenido", "Filtrar por etiqueta/fecha", FeatureStatus.COMING_SOON, "Metadatos avanzados"),
    FeatureItem("Contenido", "Orden manual de fotos", FeatureStatus.COMING_SOON, "Arrastrar y soltar"),
  // Música
    FeatureItem("Música", "Playlist propia / carpeta", FeatureStatus.IMPLEMENTED, "Carpetas y biblioteca"),
    FeatureItem("Música", "Volumen independiente", FeatureStatus.IMPLEMENTED, "Slider en dashboard"),
    FeatureItem("Música", "Ducking en vídeos", FeatureStatus.IMPLEMENTED, "Pausa o bajar volumen"),
    FeatureItem("Música", "Fade in/out entre pistas", FeatureStatus.PARTIAL, "Crossfade básico"),
    FeatureItem("Música", "Sincronizar ritmo con foto", FeatureStatus.COMING_SOON, "Análisis de BPM"),
    FeatureItem("Música", "Silencio horario nocturno", FeatureStatus.COMING_SOON, "Modo horario"),
  // Visual
    FeatureItem("Visual", "Marco dorado", FeatureStatus.IMPLEMENTED, "Pantalla completa"),
    FeatureItem("Visual", "Reloj / fecha superpuesto", FeatureStatus.IMPLEMENTED, "Configurable"),
    FeatureItem("Visual", "Tipos de marco (madera, metal…)", FeatureStatus.COMING_SOON, "Solo dorado por ahora"),
    FeatureItem("Visual", "Filtros B/N, cálido, película", FeatureStatus.COMING_SOON, "GPU shaders"),
    FeatureItem("Visual", "Brillo / contraste", FeatureStatus.PARTIAL, "Brillo en config"),
  // Horario / Pantalla
    FeatureItem("Horario", "Activación por hora", FeatureStatus.COMING_SOON, "Programador"),
    FeatureItem("Horario", "Modo día / noche", FeatureStatus.COMING_SOON, "Álbumes por franja"),
    FeatureItem("Pantalla", "Overscan / zoom TV", FeatureStatus.IMPLEMENTED, "Borde rosa y zoom"),
    FeatureItem("Pantalla", "Orientación vertical", FeatureStatus.COMING_SOON, "Rotación forzada"),
    FeatureItem("Privacidad", "PIN para ajustes", FeatureStatus.COMING_SOON, "Bloqueo parental")
)

@Composable
fun ComingSoonPanel(feature: RoadmapFeature) {
    val device = LocalDeviceProfile.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (device.isTv) Modifier.focusGroup() else Modifier)
    ) {
        Text(feature.title, style = MaterialTheme.typography.titleLarge, color = MemoriaInk, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MemoriaPurpleSoft,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Próximamente",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MemoriaPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            roadmapBlurb(feature),
            color = MemoriaMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        // weight(1f) acota la altura para que LazyColumn pueda hacer scroll
        FeatureCatalogSection(
            filterCategory = null,
            highlight = feature.title,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FeatureCatalogPanel() {
    FeatureCatalogSection(
        filterCategory = null,
        highlight = null,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun FeatureCatalogSection(
    filterCategory: String?,
    highlight: String?,
    modifier: Modifier = Modifier
) {
    val catalogItems = memoriaFeatureCatalog.filter { filterCategory == null || it.category == filterCategory }
    val device = LocalDeviceProfile.current
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .then(if (device.isTv) Modifier.focusGroup() else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(catalogItems, key = { "${it.category}_${it.name}" }) { item ->
            val emphasized = highlight != null && item.name.contains(highlight, ignoreCase = true)
            val focusRequester = remember { FocusRequester() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (device.isTv) Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                        else Modifier
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (emphasized) MemoriaPurpleSoft else MemoriaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MemoriaLine)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        when (item.status) {
                            FeatureStatus.IMPLEMENTED -> Icons.Default.CheckCircle
                            FeatureStatus.PARTIAL -> Icons.Default.Schedule
                            FeatureStatus.COMING_SOON -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (item.status) {
                            FeatureStatus.IMPLEMENTED -> MemoriaPurple
                            FeatureStatus.PARTIAL -> MemoriaMuted
                            FeatureStatus.COMING_SOON -> MemoriaMuted.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = MemoriaInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(item.note, color = MemoriaMuted, fontSize = 12.sp)
                    }
                    Text(
                        when (item.status) {
                            FeatureStatus.IMPLEMENTED -> "Listo"
                            FeatureStatus.PARTIAL -> "Parcial"
                            FeatureStatus.COMING_SOON -> "Soon"
                        },
                        color = MemoriaMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun roadmapBlurb(feature: RoadmapFeature): String = when (feature) {
    RoadmapFeature.VIDEOS -> "Vista dedicada para gestionar y reproducir solo vídeos del álbum."
    RoadmapFeature.SCHEDULE -> "Programa encendido, álbum por franja horaria y apagado automático."
    RoadmapFeature.VISUAL_THEME -> "Temas de color, tipografía y estilo del dashboard."
    RoadmapFeature.CLOUD_SOURCES -> "Importa desde Google Photos, URLs y servicios externos."
    RoadmapFeature.TRANSITIONS_ADV -> "Glitch, cortina y transiciones generativas adicionales."
    RoadmapFeature.PRESENCE -> "Activa el marco cuando detecte movimiento en la habitación."
    RoadmapFeature.MULTI_SCREEN -> "Varias pantallas, modo espejo o contenido independiente."
    RoadmapFeature.PRIVACY_PIN -> "Protege ajustes con PIN y modo invitado sin configuración."
    RoadmapFeature.ICLOUD -> "Sincronización con iCloud y Dropbox."
    RoadmapFeature.BEAT_SYNC -> "Cambia de foto al ritmo de la música."
    RoadmapFeature.FILTERS -> "Blanco y negro, cálido y look película analógica."
    RoadmapFeature.GUEST_MODE -> "Comparte álbumes con familiares de forma segura."
}
