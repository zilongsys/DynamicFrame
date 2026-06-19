package com.dynamicframe.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.ui.theme.MemoriaInk
import com.dynamicframe.ui.theme.MemoriaLine
import com.dynamicframe.ui.theme.MemoriaMuted
import com.dynamicframe.ui.theme.MemoriaPurple
import com.dynamicframe.ui.theme.MemoriaPurpleSoft
import com.dynamicframe.ui.theme.MemoriaSurface
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.safeClickable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class FeatureStatus { IMPLEMENTED, PARTIAL, COMING_SOON }

data class FeatureItem(
    val category: String,
    val name: String,
    val status: FeatureStatus,
    val note: String
)

val memoriaFeatureCatalog: List<FeatureItem> = listOf(
    FeatureItem("Reproducción", "Duración por foto (3–120 s)", FeatureStatus.IMPLEMENTED, "Ajustes e intervalo en dashboard"),
    FeatureItem("Reproducción", "Transiciones (fundido, Ken Burns, deslizar…)", FeatureStatus.IMPLEMENTED, "20+ tipos disponibles"),
    FeatureItem("Reproducción", "Orden aleatorio", FeatureStatus.IMPLEMENTED, "Toggle en dashboard"),
    FeatureItem("Reproducción", "Bucle continuo", FeatureStatus.IMPLEMENTED, "Toggle en dashboard"),
    FeatureItem("Reproducción", "Mezcla fotos + vídeos", FeatureStatus.IMPLEMENTED, "Filtro de contenido"),
    FeatureItem("Reproducción", "Velocidad de transición", FeatureStatus.IMPLEMENTED, "Duración en ms en ajustes"),
    FeatureItem("Reproducción", "Transición glitch / cortina", FeatureStatus.COMING_SOON, "Próxima versión"),
    FeatureItem("Contenido", "Galería local / carpetas SAF", FeatureStatus.IMPLEMENTED, "USB y almacenamiento interno"),
    FeatureItem("Contenido", "Álbumes múltiples", FeatureStatus.IMPLEMENTED, "Píldoras y selección"),
    FeatureItem("Contenido", "Google Photos", FeatureStatus.COMING_SOON, "Requiere API OAuth"),
    FeatureItem("Contenido", "iCloud / Dropbox", FeatureStatus.COMING_SOON, "Integración en nube"),
    FeatureItem("Contenido", "Filtrar por etiqueta/fecha", FeatureStatus.COMING_SOON, "Metadatos avanzados"),
    FeatureItem("Contenido", "Orden manual de fotos", FeatureStatus.COMING_SOON, "Arrastrar y soltar"),
    FeatureItem("Música", "Playlist propia / carpeta", FeatureStatus.IMPLEMENTED, "Carpetas y biblioteca"),
    FeatureItem("Música", "Volumen independiente", FeatureStatus.IMPLEMENTED, "Slider en dashboard"),
    FeatureItem("Música", "Ducking en vídeos", FeatureStatus.IMPLEMENTED, "Pausa o bajar volumen"),
    FeatureItem("Música", "Fade in/out entre pistas", FeatureStatus.PARTIAL, "Crossfade básico"),
    FeatureItem("Música", "Sincronizar ritmo con foto", FeatureStatus.COMING_SOON, "Análisis de BPM"),
    FeatureItem("Música", "Silencio horario nocturno", FeatureStatus.COMING_SOON, "Modo horario"),
    FeatureItem("Visual", "Marco dorado", FeatureStatus.IMPLEMENTED, "Pantalla completa"),
    FeatureItem("Visual", "Reloj / fecha superpuesto", FeatureStatus.IMPLEMENTED, "Configurable"),
    FeatureItem("Visual", "Tipos de marco (madera, metal…)", FeatureStatus.COMING_SOON, "Solo dorado por ahora"),
    FeatureItem("Visual", "Filtros B/N, cálido, película", FeatureStatus.COMING_SOON, "GPU shaders"),
    FeatureItem("Visual", "Brillo / contraste", FeatureStatus.PARTIAL, "Brillo en config"),
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
        Text(feature.title, style = MaterialTheme.typography.titleLarge,
            color = MemoriaInk, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Surface(color = MemoriaPurpleSoft, shape = RoundedCornerShape(8.dp)) {
            Text("Próximamente",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MemoriaPurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        Text(roadmapBlurb(feature), color = MemoriaMuted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        FeatureCatalogSection(filterCategory = null, highlight = feature.title,
            modifier = Modifier.weight(1f))
    }
}

@Composable
fun FeatureCatalogPanel() {
    FeatureCatalogSection(filterCategory = null, highlight = null,
        modifier = Modifier.fillMaxSize())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeatureCatalogSection(
    filterCategory: String?,
    highlight: String?,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    val listState = rememberLazyListState()
    val firstItemFocus = remember { FocusRequester() }

    // Memoizado por filterCategory: si no, 'allItems' se recrea en cada recomposición y
    // reinicia el LaunchedEffect de abajo provocando un scrollToItem(0) constante (el "salto").
    val allItems = remember(filterCategory) {
        memoriaFeatureCatalog
            .filter { filterCategory == null || it.category == filterCategory }
    }

    var statusFilter by remember { mutableStateOf<FeatureStatus?>(null) }
    val filteredItems = remember(statusFilter, allItems) {
        if (statusFilter == null) allItems else allItems.filter { it.status == statusFilter }
    }

    val scope = rememberCoroutineScope()
    var listHadFocus by remember { mutableStateOf(false) }

    // Índice del ítem actualmente enfocado; usado para scroll controlado (TV).
    var focusedItemIdx by remember { mutableIntStateOf(0) }

    // Al cambiar de filtro reposiciona la lista arriba y resetea el índice enfocado.
    LaunchedEffect(statusFilter) {
        if (device.isTv) {
            focusedItemIdx = 0
            runCatching { listState.scrollToItem(0) }
        }
    }

    // Scroll controlado sin BringIntoViewSpec: solo desplaza cuando el ítem enfocado
    // está fuera del área visible, evitando el efecto de "salto" que causaba el spec
    // anterior al competir con el sistema de foco nativo de TV.
    LaunchedEffect(focusedItemIdx) {
        if (!device.isTv) return@LaunchedEffect
        val info = snapshotFlow { listState.layoutInfo }
            .first { it.visibleItemsInfo.isNotEmpty() }
        val firstVisible = info.visibleItemsInfo.first().index
        val lastVisible = info.visibleItemsInfo.last().index
        if (focusedItemIdx !in firstVisible..lastVisible) {
            listState.animateScrollToItem(maxOf(0, focusedItemIdx - 1))
        }
    }

    Column(modifier = modifier) {
        // ── Filtros de estado ───────────────────────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                RoadmapFilterChip(
                    selected = statusFilter == null,
                    label = "Todos",
                    color = MemoriaPurple,
                    onClick = { statusFilter = null }
                )
            }
            item {
                RoadmapFilterChip(
                    selected = statusFilter == FeatureStatus.IMPLEMENTED,
                    label = "✓ Listo",
                    color = MemoriaPurple,
                    onClick = {
                        statusFilter = if (statusFilter == FeatureStatus.IMPLEMENTED) null
                        else FeatureStatus.IMPLEMENTED
                    }
                )
            }
            item {
                RoadmapFilterChip(
                    selected = statusFilter == FeatureStatus.PARTIAL,
                    label = "◑ Parcial",
                    color = MemoriaMuted,
                    onClick = {
                        statusFilter = if (statusFilter == FeatureStatus.PARTIAL) null
                        else FeatureStatus.PARTIAL
                    }
                )
            }
            item {
                RoadmapFilterChip(
                    selected = statusFilter == FeatureStatus.COMING_SOON,
                    label = "◌ Próximamente",
                    color = MemoriaMuted,
                    onClick = {
                        statusFilter = if (statusFilter == FeatureStatus.COMING_SOON) null
                        else FeatureStatus.COMING_SOON
                    }
                )
            }
        }

        // ── Lista con barra de desplazamiento ───────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 14.dp)
                    .then(
                        if (device.isTv) Modifier
                            // Al ENTRAR en la lista (gana el foco), va siempre a la primera fila.
                            .onFocusChanged { fs ->
                                if (fs.hasFocus && !listHadFocus) {
                                    scope.launch {
                                        focusedItemIdx = 0
                                        listState.scrollToItem(0)
                                        runCatching { firstItemFocus.requestFocus() }
                                    }
                                }
                                listHadFocus = fs.hasFocus
                            }
                            .focusGroup()
                        else Modifier
                    )
            ) {
                itemsIndexed(
                    filteredItems,
                    key = { _, it -> "${it.category}_${it.name}" }
                ) { index, item ->
                    val emphasized = highlight != null &&
                        item.name.contains(highlight, ignoreCase = true)
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused by interactionSource.collectIsFocusedAsState()

                    // Foco = color morado completo (como los botones principales).
                    val bgColor by animateColorAsState(
                        targetValue = when {
                            focused -> MemoriaPurple
                            emphasized -> MemoriaPurpleSoft
                            else -> MemoriaSurface
                        },
                        animationSpec = tween(durationMillis = 140),
                        label = "roadmap-item-bg"
                    )
                    // Contenido en blanco cuando la fila está enfocada (sobre morado).
                    val onBg = if (focused) Color.White else MemoriaInk
                    val onBgMuted = if (focused) Color.White.copy(alpha = 0.85f) else MemoriaMuted

                    val rowModifier = Modifier
                        .fillMaxWidth()
                        // Espaciado como MARGEN (antes de focusable) → queda fuera del área
                        // enfocable y el auto-scroll por foco no sobre-desplaza = scroll natural.
                        .padding(bottom = 8.dp)
                        .then(
                            if (device.isTv && index == 0) Modifier.focusRequester(firstItemFocus)
                            else Modifier
                        )
                        // Trackea qué índice está enfocado para el scroll controlado.
                        .then(
                            if (device.isTv) Modifier.onFocusChanged { fs ->
                                if (fs.isFocused) focusedItemIdx = index
                            } else Modifier
                        )
                        .then(
                            if (device.isTv) Modifier.focusable(
                                interactionSource = interactionSource
                            ) else Modifier
                        )

                    Surface(
                        modifier = rowModifier,
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        // Sin borde cuando está enfocada (el morado ya destaca); fino si no.
                        border = if (focused) null
                        else androidx.compose.foundation.BorderStroke(1.dp, MemoriaLine)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when (item.status) {
                                    FeatureStatus.IMPLEMENTED -> Icons.Default.CheckCircle
                                    FeatureStatus.PARTIAL, FeatureStatus.COMING_SOON ->
                                        Icons.Default.Schedule
                                },
                                contentDescription = null,
                                tint = if (focused) Color.White else when (item.status) {
                                    FeatureStatus.IMPLEMENTED -> MemoriaPurple
                                    FeatureStatus.PARTIAL -> MemoriaMuted
                                    FeatureStatus.COMING_SOON -> MemoriaMuted.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                // Peso de fuente CONSTANTE + maxLines → la altura de la fila no
                                // cambia al enfocar, eliminando el "brinco" al subir/bajar.
                                Text(item.name, color = onBg,
                                    fontSize = if (device.isTv) 18.sp else 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text(item.note, color = onBgMuted,
                                    fontSize = if (device.isTv) 14.sp else 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                if (item.category.isNotBlank()) {
                                    Text(item.category,
                                        color = if (focused) Color.White.copy(alpha = 0.7f)
                                        else MemoriaMuted.copy(alpha = 0.7f),
                                        fontSize = if (device.isTv) 12.sp else 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Surface(
                                color = if (focused) Color.White.copy(alpha = 0.22f)
                                else when (item.status) {
                                    FeatureStatus.IMPLEMENTED -> MemoriaPurpleSoft
                                    FeatureStatus.PARTIAL -> MemoriaLine
                                    FeatureStatus.COMING_SOON -> MemoriaLine.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when (item.status) {
                                        FeatureStatus.IMPLEMENTED -> "Listo"
                                        FeatureStatus.PARTIAL -> "Parcial"
                                        FeatureStatus.COMING_SOON -> "Próx."
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = if (focused) Color.White else when (item.status) {
                                        FeatureStatus.IMPLEMENTED -> MemoriaPurple
                                        else -> MemoriaMuted
                                    },
                                    fontSize = if (device.isTv) 13.sp else 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── Barra de desplazamiento vertical ────────────────────────────
            RoadmapScrollbar(
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun RoadmapFilterChip(
    selected: Boolean,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)

    // Activo = violeta completo (todos) · Enfocado (no activo) = gris · Reposo = contorno.
    val bg by animateColorAsState(
        targetValue = when {
            selected -> MemoriaPurple
            focused -> MemoriaMuted
            else -> MemoriaSurface
        },
        animationSpec = tween(120),
        label = "chip-bg"
    )
    val textColor = when {
        selected || focused -> Color.White
        else -> MemoriaMuted
    }
    // Anillo de foco: SIEMPRE que esté enfocado (incluido el chip activo) se ve un borde claro,
    // así se distingue sobre cuál estás aunque ya esté en violeta. Reposo no activo = contorno fino.
    val borderColor = when {
        focused -> Color.White
        selected -> MemoriaPurple
        else -> MemoriaLine
    }
    val borderWidth = if (focused) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(borderWidth, borderColor, shape)
            .safeClickable(
                interactionSource = interactionSource,
                showFocusBorder = false,
                focusScale = false,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RoadmapScrollbar(
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    val visibleCount by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }
    }

    if (totalItems <= visibleCount || totalItems == 0) return

    val thumbFraction = (visibleCount.toFloat() / totalItems).coerceIn(0.08f, 1f)
    val scrollFraction = (firstVisible.toFloat() /
        (totalItems - visibleCount).coerceAtLeast(1)).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .width(6.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(MemoriaLine.copy(alpha = 0.35f))
    ) {
        val thumbHeight = maxHeight * thumbFraction
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(thumbHeight)
                .offset(y = thumbOffset)
                .clip(RoundedCornerShape(3.dp))
                .background(MemoriaPurple.copy(alpha = 0.7f))
        )
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
