package com.dynamicframe.domain.model

enum class MediaSource {
    LOCAL,
    GOOGLE_PHOTOS,
    ONEDRIVE,
    NETWORK_SHARE
}

data class MediaItem(
    val id: String,
    val uri: String,
    val type: MediaType,
    val source: MediaSource,
    val name: String = "",
    val dateAdded: Long = 0L,
    val duration: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val albumId: String = "",
    val albumName: String = "",
    val thumbnailUri: String? = null
)

enum class MediaType {
    IMAGE, VIDEO
}

/** Qué tipos de archivo mostrar en el slideshow */
enum class MediaContentFilter {
    ALL, PHOTOS_ONLY, VIDEOS_ONLY;

    fun allows(type: MediaType): Boolean = when (this) {
        ALL -> true
        PHOTOS_ONLY -> type == MediaType.IMAGE
        VIDEOS_ONLY -> type == MediaType.VIDEO
    }
}

data class MediaAlbum(
    val id: String,
    val name: String,
    val source: MediaSource,
    val coverUri: String?,
    val itemCount: Int = 0
)

data class MusicTrack(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String = "Desconocido",
    val album: String = "",
    val duration: Long = 0L,
    val albumArtUri: String? = null
)

enum class MusicSourceType {
    LOCAL_FOLDER,
    DEVICE_LIBRARY,
    THEME,
    SPOTIFY,
    YOUTUBE
}

enum class MusicTheme {
    RELAX, ENERGETIC, CLASSIC, NATURE, AMBIENT
}

/** Comportamiento de la música de fondo cuando hay un video */
enum class VideoMusicBehavior {
    PAUSE,
    DUCK
}

data class SlideshowConfig(
    val intervalSeconds: Int = 8,
    val transition: TransitionType = TransitionType.CROSSFADE,
    /** @deprecated Usar [photoShuffle] y [videoShuffle]. Se mantiene solo para migración. */
    val shuffle: Boolean = true,
    val photoShuffle: Boolean = true,
    val videoShuffle: Boolean = true,
    val loop: Boolean = true,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val clockPosition: ClockPosition = ClockPosition.BOTTOM_RIGHT,
    val musicVolume: Float = 0.4f,
    val mediaVolume: Float = 1.0f,
    val muteVideoAudio: Boolean = false,
    val videoPlayFull: Boolean = true,
    val brightness: Float = 1.0f,
    val selectedAlbumIds: List<String> = emptyList(),
    val musicPlaylistId: String? = null,
    val autoStartOnBoot: Boolean = false,
    val screenSaverMode: Boolean = false,
    // Carpetas locales (SAF / file). Vacías = galería del dispositivo
    val photoFolderUris: List<String> = emptyList(),
    val videoFolderUris: List<String> = emptyList(),
    /** URIs desactivadas temporalmente sin borrar */
    val disabledPhotoFolderUris: Set<String> = emptySet(),
    val disabledVideoFolderUris: Set<String> = emptySet(),
    val disabledMusicFolderUris: Set<String> = emptySet(),
    val mediaContentFilter: MediaContentFilter = MediaContentFilter.ALL,
    // Música de fondo — múltiples fuentes activas
    val musicSourceTypes: Set<MusicSourceType> = setOf(MusicSourceType.DEVICE_LIBRARY),
    val musicFolderUris: List<String> = emptyList(),
    val musicTheme: MusicTheme = MusicTheme.RELAX,
    val spotifyPlaylistUrl: String = "",
    val youtubePlaylistUrl: String = "",
    val musicShuffle: Boolean = true,
    // Durante videos
    val videoMusicBehavior: VideoMusicBehavior = VideoMusicBehavior.DUCK,
    val duckedMusicVolume: Float = 0.08f,
    // Pantalla completa / reproducción
    val playbackShowClock: Boolean = true,
    val playbackShowOverlay: Boolean = true,
    val playbackImmersiveMode: Boolean = false,
    val transitionDurationMs: Int = 1400,
    // Ajuste pantalla TV (overscan / zoom)
    val showScreenBorder: Boolean = false,
    val uiScale: Float = 1.0f,
    /** Marco dorado estilo cuadro alrededor del slideshow */
    val showPictureFrame: Boolean = false,
    /** Borde rosa de zona segura solo en pantalla completa */
    val playbackShowSafeBorder: Boolean = false,
    /** Grosor del marco dorado en pantalla completa (0.5 = fino, 1.5 = grueso) */
    val playbackPictureFrameScale: Float = 1.0f,
    /** Zoom del contenido en pantalla completa (1.0 = borde a borde) */
    val playbackContentZoom: Float = 1.0f,
    /** Fondo detrás de fotos/vídeos (letterbox) */
    val playbackBackgroundType: PlaybackBackgroundType = PlaybackBackgroundType.DEMO_LAVENDER,
    /** URI de imagen personalizada para fondo (si type = CUSTOM_IMAGE) */
    val playbackBackgroundImageUri: String = "",
    /** Estilo de interfaz (tema) de la pantalla de reproducción */
    val playbackTheme: PlaybackTheme = PlaybackTheme.AURORA_GLASS
) {
    /** Compatibilidad: primera fuente activa, o DEVICE_LIBRARY si no hay ninguna */
    val musicSourceType: MusicSourceType
        get() = musicSourceTypes.firstOrNull() ?: MusicSourceType.DEVICE_LIBRARY

    /** Carpetas de fotos activas (excluye las desactivadas temporalmente) */
    fun activePhotoFolderUris(): List<String> =
        photoFolderUris.filter { it !in disabledPhotoFolderUris }

    /** Carpetas de vídeos activas */
    fun activeVideoFolderUris(): List<String> =
        videoFolderUris.filter { it !in disabledVideoFolderUris }

    /** Carpetas de música activas */
    fun activeMusicFolderUris(): List<String> =
        musicFolderUris.filter { it !in disabledMusicFolderUris }

}

/** Fondo visible en los márgenes cuando la foto no llena el marco */
enum class PlaybackBackgroundType {
    BLACK,
    DEMO_LAVENDER,
    DEMO_SUNSET,
    DEMO_MIDNIGHT,
    CUSTOM_IMAGE
}

/**
 * Estilo de interfaz de la pantalla de reproducción.
 * - [AURORA_GLASS]: glassmorphism unificado con HUD inferior (por defecto).
 * - [AMBIENT]: minimalismo cinematográfico, controles mínimos centrados.
 * - [GALLERY]: marco editorial tipo museo, riel lateral + placa de datos.
 */
enum class PlaybackTheme {
    AURORA_GLASS,
    AMBIENT,
    GALLERY
}

enum class TransitionType {
    CROSSFADE,
    FADE,
    DISSOLVE,
    KEN_BURNS,
    BLUR_FADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    ZOOM_IN,
    ZOOM_OUT,
    ROTATE,
    FLIP_HORIZONTAL,
    FLIP_VERTICAL,
    WIPE_LEFT,
    WIPE_RIGHT,
    DEPTH,
    STACK,
    PARALLAX,
    CUBE,
    NONE
}

enum class ClockPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

data class SlideshowState(
    val isPlaying: Boolean = false,
    val currentItem: MediaItem? = null,
    val nextItem: MediaItem? = null,
    val currentIndex: Int = 0,
    val totalItems: Int = 0,
    val allItems: List<MediaItem> = emptyList(),
    val playlistItems: List<MediaItem> = emptyList(),
    val isTransitioning: Boolean = false,
    /**
     * Contador que se incrementa en cada navegación. Permite reiniciar la
     * reproducción del MISMO vídeo (p. ej. bucle con un único vídeo) aunque la
     * URI no cambie, ya que el reproductor reacciona al cambio de este token.
     */
    val playToken: Int = 0,
    val error: String? = null
)

data class MusicPlayerState(
    val isPlaying: Boolean = false,
    val currentTrack: MusicTrack? = null,
    val playlist: List<MusicTrack> = emptyList(),
    val currentIndex: Int = 0,
    val volume: Float = 0.4f,
    val isShuffle: Boolean = true,
    val isDucked: Boolean = false
)
