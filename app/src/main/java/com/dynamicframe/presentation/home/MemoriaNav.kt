package com.dynamicframe.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Destinos del panel lateral MEMORIA */
sealed class MemoriaDestination {
    data object AlbumActive : MemoriaDestination()
    data object Albums : MemoriaDestination()
    data object Music : MemoriaDestination()
    data object Settings : MemoriaDestination()
    data object FeatureCatalog : MemoriaDestination()
    data class Roadmap(val feature: RoadmapFeature) : MemoriaDestination()
}

enum class RoadmapFeature(
    val title: String,
    val icon: ImageVector,
    val group: RoadmapGroup
) {
    VIDEOS("Videos", Icons.Default.Videocam, RoadmapGroup.BIBLIOTECA),
    SCHEDULE("Horario", Icons.Default.Schedule, RoadmapGroup.CONFIGURAR),
    VISUAL_THEME("Tema visual", Icons.Default.Palette, RoadmapGroup.CONFIGURAR),
    CLOUD_SOURCES("Fuentes en la nube", Icons.Default.Cloud, RoadmapGroup.CONFIGURAR),
    TRANSITIONS_ADV("Transiciones avanzadas", Icons.Default.AutoAwesome, RoadmapGroup.CONFIGURAR),
    PRESENCE("Sensor de presencia", Icons.Default.Sensors, RoadmapGroup.CONFIGURAR),
    MULTI_SCREEN("Múltiples pantallas", Icons.Default.Cast, RoadmapGroup.SISTEMA),
    PRIVACY_PIN("PIN y modo invitado", Icons.Default.Lock, RoadmapGroup.SISTEMA),
    ICLOUD("iCloud / Dropbox", Icons.Default.CloudUpload, RoadmapGroup.CONFIGURAR),
    BEAT_SYNC("Sincronizar ritmo con foto", Icons.Default.GraphicEq, RoadmapGroup.CONFIGURAR),
    FILTERS("Filtros analógicos", Icons.Default.FilterVintage, RoadmapGroup.CONFIGURAR),
    GUEST_MODE("Compartir con familia", Icons.Default.Group, RoadmapGroup.SISTEMA)
}

enum class RoadmapGroup(val label: String) {
    BIBLIOTECA("Biblioteca"),
    CONFIGURAR("Configurar"),
    SISTEMA("Sistema")
}

data class MemoriaNavEntry(
    val destination: MemoriaDestination,
    val label: String,
    val icon: ImageVector,
    val group: RoadmapGroup,
    val badge: String? = null
)

val memoriaSidebarEntries: List<MemoriaNavEntry> = listOf(
    MemoriaNavEntry(MemoriaDestination.AlbumActive, "Álbum activo", Icons.Default.PhotoAlbum, RoadmapGroup.BIBLIOTECA),
    MemoriaNavEntry(MemoriaDestination.Albums, "Mis álbumes", Icons.Default.PhotoLibrary, RoadmapGroup.BIBLIOTECA),
    MemoriaNavEntry(MemoriaDestination.Roadmap(RoadmapFeature.VIDEOS), "Videos", Icons.Default.Videocam, RoadmapGroup.BIBLIOTECA),
    MemoriaNavEntry(MemoriaDestination.Music, "Música", Icons.Default.MusicNote, RoadmapGroup.BIBLIOTECA),
    MemoriaNavEntry(MemoriaDestination.Roadmap(RoadmapFeature.TRANSITIONS_ADV), "Transiciones", Icons.Default.AutoAwesome, RoadmapGroup.CONFIGURAR),
    MemoriaNavEntry(MemoriaDestination.Roadmap(RoadmapFeature.SCHEDULE), "Horario", Icons.Default.Schedule, RoadmapGroup.CONFIGURAR),
    MemoriaNavEntry(MemoriaDestination.Roadmap(RoadmapFeature.VISUAL_THEME), "Tema visual", Icons.Default.Palette, RoadmapGroup.CONFIGURAR),
    MemoriaNavEntry(MemoriaDestination.Roadmap(RoadmapFeature.CLOUD_SOURCES), "Fuentes", Icons.Default.FolderOpen, RoadmapGroup.CONFIGURAR),
    MemoriaNavEntry(MemoriaDestination.Roadmap(RoadmapFeature.MULTI_SCREEN), "Pantalla", Icons.Default.Tv, RoadmapGroup.SISTEMA),
    MemoriaNavEntry(MemoriaDestination.Settings, "Preferencias", Icons.Default.Settings, RoadmapGroup.SISTEMA),
    MemoriaNavEntry(MemoriaDestination.FeatureCatalog, "Hoja de ruta", Icons.Default.Checklist, RoadmapGroup.SISTEMA)
)
