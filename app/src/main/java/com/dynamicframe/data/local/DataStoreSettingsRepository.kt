package com.dynamicframe.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.dynamicframe.di.SlideshowSettingsDataStore
import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.model.allMediaFolderUris
import com.dynamicframe.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @SlideshowSettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    @Volatile
    private var cachedConfig: SlideshowConfig = SlideshowConfig()

    private val Keys get() = SlideshowPreferencesKeys

    override fun observeConfig(): Flow<SlideshowConfig> =
        dataStore.data
            .map { prefs -> prefs.toConfig().also { cachedConfig = it } }
            .catch {
                emit(cachedConfig)
            }

    override suspend fun getConfig(): SlideshowConfig = runCatching {
        dataStore.data.first().toConfig().also { cachedConfig = it }
    }.getOrElse { cachedConfig }

    override suspend fun saveConfig(config: SlideshowConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.INTERVAL] = config.intervalSeconds
            prefs[Keys.TRANSITION] = config.transition.name
            prefs[Keys.SHUFFLE] = config.photoShuffle && config.videoShuffle
            prefs[Keys.PHOTO_SHUFFLE] = config.photoShuffle
            prefs[Keys.VIDEO_SHUFFLE] = config.videoShuffle
            prefs[Keys.LOOP] = config.loop
            prefs[Keys.SHOW_CLOCK] = config.showClock
            prefs[Keys.SHOW_DATE] = config.showDate
            prefs[Keys.CLOCK_POSITION] = config.clockPosition.name
            prefs[Keys.MUSIC_VOLUME] = config.musicVolume
            prefs[Keys.MEDIA_VOLUME] = config.mediaVolume
            prefs[Keys.MUTE_VIDEO] = config.muteVideoAudio
            prefs[Keys.VIDEO_PLAY_FULL] = config.videoPlayFull
            prefs[Keys.VIDEO_DYNAMIC_BACKDROP] = config.videoDynamicBackdropMode.name
            prefs[Keys.BRIGHTNESS] = config.brightness
            prefs[Keys.SELECTED_ALBUMS] = config.selectedAlbumIds.joinToString("|")
            prefs[Keys.MUSIC_PLAYLIST] = config.musicPlaylistId ?: ""
            prefs[Keys.AUTO_START] = config.autoStartOnBoot
            prefs[Keys.SCREENSAVER] = config.screenSaverMode
            prefs[Keys.PHOTO_FOLDERS] = config.photoFolderUris.joinToString("|")
            prefs[Keys.VIDEO_FOLDERS] = config.videoFolderUris.joinToString("|")
            prefs[Keys.MEDIA_FOLDERS] = config.allMediaFolderUris().joinToString("|")
            prefs[Keys.MEDIA_CONTENT_FILTER] = config.mediaContentFilter.name
            prefs[Keys.DISABLED_PHOTO_FOLDERS] = config.disabledPhotoFolderUris.joinToString("|")
            prefs[Keys.DISABLED_VIDEO_FOLDERS] = config.disabledVideoFolderUris.joinToString("|")
            prefs[Keys.DISABLED_MUSIC_FOLDERS] = config.disabledMusicFolderUris.joinToString("|")
            prefs[Keys.MUSIC_SOURCES] = config.musicSourceTypes.joinToString("|") { it.name }
            prefs[Keys.MUSIC_SOURCE] = config.musicSourceType.name
            prefs[Keys.MUSIC_FOLDERS] = config.musicFolderUris.joinToString("|")
            prefs[Keys.MUSIC_FOLDER] = config.musicFolderUris.firstOrNull() ?: ""
            prefs[Keys.MUSIC_THEME] = config.musicTheme.name
            prefs[Keys.SPOTIFY_URL] = config.spotifyPlaylistUrl
            prefs[Keys.YOUTUBE_URL] = config.youtubePlaylistUrl
            prefs[Keys.MUSIC_SHUFFLE] = config.musicShuffle
            prefs[Keys.VIDEO_MUSIC_BEHAVIOR] = config.videoMusicBehavior.name
            prefs[Keys.DUCKED_VOLUME] = config.duckedMusicVolume
            prefs[Keys.PLAYBACK_SHOW_CLOCK] = config.playbackShowClock
            prefs[Keys.PLAYBACK_SHOW_OVERLAY] = config.playbackShowOverlay
            prefs[Keys.PLAYBACK_IMMERSIVE] = config.playbackImmersiveMode
            prefs[Keys.TRANSITION_DURATION_MS] = config.transitionDurationMs
            prefs[Keys.SHOW_SCREEN_BORDER] = config.showScreenBorder
            prefs[Keys.UI_SCALE] = config.uiScale
            prefs[Keys.SHOW_PICTURE_FRAME] = config.showPictureFrame
            prefs[Keys.PLAYBACK_SAFE_BORDER] = config.playbackShowSafeBorder
            prefs[Keys.PLAYBACK_FRAME_SCALE] = config.playbackPictureFrameScale
            prefs[Keys.PLAYBACK_CONTENT_ZOOM] = config.playbackContentZoom
            prefs[Keys.PLAYBACK_BACKGROUND] = config.playbackBackgroundType.name
            prefs[Keys.PLAYBACK_BACKGROUND_IMAGE] = config.playbackBackgroundImageUri
            prefs[Keys.PLAYBACK_THEME] = config.playbackTheme.name
            prefs[Keys.APP_THEME] = config.appTheme.name
        }
        syncBootPreference(config.autoStartOnBoot)
        cachedConfig = config
    }

    override suspend fun updateInterval(seconds: Int) {
        dataStore.edit { it[Keys.INTERVAL] = seconds }
    }

    override suspend fun updateTransition(type: TransitionType) {
        dataStore.edit { it[Keys.TRANSITION] = type.name }
    }

    override suspend fun updateMusicVolume(volume: Float) {
        dataStore.edit { it[Keys.MUSIC_VOLUME] = volume }
    }

    override suspend fun toggleShuffle(enabled: Boolean) {
        dataStore.edit {
            it[Keys.SHUFFLE] = enabled
            it[Keys.PHOTO_SHUFFLE] = enabled
            it[Keys.VIDEO_SHUFFLE] = enabled
        }
    }

    override suspend fun toggleClock(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_CLOCK] = enabled }
    }

    override suspend fun updateSelectedAlbums(albumIds: List<String>) {
        dataStore.edit { it[Keys.SELECTED_ALBUMS] = albumIds.joinToString("|") }
    }

    private fun syncBootPreference(autoStart: Boolean) {
        context.getSharedPreferences(SettingsBootCache.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SettingsBootCache.AUTO_START_BOOT, autoStart)
            .apply()
    }

    private fun Preferences.toConfig(): SlideshowConfig {
        val legacyShuffle = this[Keys.SHUFFLE] ?: true
        val photoShuffle = this[Keys.PHOTO_SHUFFLE] ?: legacyShuffle
        val videoShuffle = this[Keys.VIDEO_SHUFFLE] ?: legacyShuffle
        return SlideshowConfig(
        intervalSeconds = this[Keys.INTERVAL] ?: 8,
        transition = enumOrDefault(this[Keys.TRANSITION], TransitionType.CROSSFADE),
        shuffle = photoShuffle && videoShuffle,
        photoShuffle = photoShuffle,
        videoShuffle = videoShuffle,
        loop = this[Keys.LOOP] ?: true,
        showClock = this[Keys.SHOW_CLOCK] ?: true,
        showDate = this[Keys.SHOW_DATE] ?: true,
        clockPosition = enumOrDefault(this[Keys.CLOCK_POSITION], ClockPosition.BOTTOM_RIGHT),
        musicVolume = this[Keys.MUSIC_VOLUME] ?: 0.4f,
        mediaVolume = this[Keys.MEDIA_VOLUME] ?: 1.0f,
        muteVideoAudio = this[Keys.MUTE_VIDEO] ?: false,
        videoPlayFull = this[Keys.VIDEO_PLAY_FULL] ?: true,
        videoDynamicBackdropMode = enumOrDefault(
            this[Keys.VIDEO_DYNAMIC_BACKDROP],
            VideoDynamicBackdropMode.STATIC,
        ),
        brightness = this[Keys.BRIGHTNESS] ?: 1.0f,
        selectedAlbumIds = splitList(this[Keys.SELECTED_ALBUMS]),
        musicPlaylistId = this[Keys.MUSIC_PLAYLIST]?.ifBlank { null },
        autoStartOnBoot = this[Keys.AUTO_START] ?: false,
        screenSaverMode = this[Keys.SCREENSAVER] ?: false,
        photoFolderUris = resolvePhotoFolders(this),
        videoFolderUris = resolveVideoFolders(this),
        mediaContentFilter = enumOrDefault(this[Keys.MEDIA_CONTENT_FILTER], MediaContentFilter.ALL),
        disabledPhotoFolderUris = splitSet(this[Keys.DISABLED_PHOTO_FOLDERS]),
        disabledVideoFolderUris = splitSet(this[Keys.DISABLED_VIDEO_FOLDERS]),
        disabledMusicFolderUris = splitSet(this[Keys.DISABLED_MUSIC_FOLDERS]),
        musicSourceTypes = splitSet(this[Keys.MUSIC_SOURCES]).mapNotNull {
            runCatching { MusicSourceType.valueOf(it) }.getOrNull()
        }.toSet().ifEmpty { setOf(enumOrDefault(this[Keys.MUSIC_SOURCE], MusicSourceType.DEVICE_LIBRARY)) },
        // musicSourceType is computed from musicSourceTypes
        musicFolderUris = resolveMusicFolders(this),
        musicTheme = enumOrDefault(this[Keys.MUSIC_THEME], MusicTheme.RELAX),
        spotifyPlaylistUrl = this[Keys.SPOTIFY_URL] ?: "",
        youtubePlaylistUrl = this[Keys.YOUTUBE_URL] ?: "",
        musicShuffle = this[Keys.MUSIC_SHUFFLE] ?: true,
        videoMusicBehavior = enumOrDefault(this[Keys.VIDEO_MUSIC_BEHAVIOR], VideoMusicBehavior.DUCK),
        duckedMusicVolume = this[Keys.DUCKED_VOLUME] ?: 0.08f,
        playbackShowClock = this[Keys.PLAYBACK_SHOW_CLOCK] ?: true,
        playbackShowOverlay = this[Keys.PLAYBACK_SHOW_OVERLAY] ?: true,
        playbackImmersiveMode = this[Keys.PLAYBACK_IMMERSIVE] ?: false,
        transitionDurationMs = this[Keys.TRANSITION_DURATION_MS] ?: 1400,
        showScreenBorder = this[Keys.SHOW_SCREEN_BORDER] ?: false,
        uiScale = this[Keys.UI_SCALE] ?: 1.0f,
        showPictureFrame = this[Keys.SHOW_PICTURE_FRAME] ?: false,
        playbackShowSafeBorder = this[Keys.PLAYBACK_SAFE_BORDER] ?: false,
        playbackPictureFrameScale = this[Keys.PLAYBACK_FRAME_SCALE] ?: 1.0f,
        playbackContentZoom = this[Keys.PLAYBACK_CONTENT_ZOOM] ?: 1.0f,
        playbackBackgroundType = enumOrDefault(this[Keys.PLAYBACK_BACKGROUND], PlaybackBackgroundType.DEMO_LAVENDER),
        playbackBackgroundImageUri = this[Keys.PLAYBACK_BACKGROUND_IMAGE] ?: "",
        playbackTheme = enumOrDefault(this[Keys.PLAYBACK_THEME], PlaybackTheme.AURORA_GLASS),
        appTheme = enumOrDefault(this[Keys.APP_THEME], AppTheme.DEFAULT)
    )
    }

    private fun splitList(raw: String?): List<String> =
        raw?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    private fun splitSet(raw: String?): Set<String> =
        raw?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    private fun resolvePhotoFolders(prefs: Preferences): List<String> {
        // Si la clave existe (aunque esté vacía) el usuario ya tiene su lista explícita: respetarla.
        // Solo se migra desde la clave legacy combinada (MEDIA_FOLDERS) si NUNCA se guardó la clave
        // específica (config previa a la migración). Así una lista vacía no se rellena con la otra.
        if (prefs[Keys.PHOTO_FOLDERS] != null) return splitList(prefs[Keys.PHOTO_FOLDERS])
        return migrateLegacyMediaFolders(prefs).first
    }

    private fun resolveVideoFolders(prefs: Preferences): List<String> {
        if (prefs[Keys.VIDEO_FOLDERS] != null) return splitList(prefs[Keys.VIDEO_FOLDERS])
        return migrateLegacyMediaFolders(prefs).second
    }

    private fun migrateLegacyMediaFolders(prefs: Preferences): Pair<List<String>, List<String>> {
        val legacy = splitList(prefs[Keys.MEDIA_FOLDERS])
        if (legacy.isEmpty()) return emptyList<String>() to emptyList()
        return when (enumOrDefault(prefs[Keys.MEDIA_CONTENT_FILTER], MediaContentFilter.ALL)) {
            MediaContentFilter.PHOTOS_ONLY -> legacy to emptyList()
            MediaContentFilter.VIDEOS_ONLY -> emptyList<String>() to legacy
            MediaContentFilter.ALL -> legacy to legacy
        }
    }

    private fun resolveMusicFolders(prefs: Preferences): List<String> {
        val explicit = splitList(prefs[Keys.MUSIC_FOLDERS])
        if (explicit.isNotEmpty()) return explicit
        return listOfNotNull(prefs[Keys.MUSIC_FOLDER]?.ifBlank { null })
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: return default) }.getOrDefault(default)
}
