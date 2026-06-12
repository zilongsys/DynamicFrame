package com.dynamicframe.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dynamicframe.domain.model.*
import com.dynamicframe.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "slideshow_settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val INTERVAL = intPreferencesKey("interval_seconds")
        val TRANSITION = stringPreferencesKey("transition_type")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val LOOP = booleanPreferencesKey("loop")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val CLOCK_POSITION = stringPreferencesKey("clock_position")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val MEDIA_VOLUME = floatPreferencesKey("media_volume")
        val MUTE_VIDEO = booleanPreferencesKey("mute_video")
        val VIDEO_PLAY_FULL = booleanPreferencesKey("video_play_full")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val SELECTED_ALBUMS = stringPreferencesKey("selected_album_ids")
        val MUSIC_PLAYLIST = stringPreferencesKey("music_playlist_id")
        val AUTO_START = booleanPreferencesKey("auto_start_boot")
        val SCREENSAVER = booleanPreferencesKey("screensaver_mode")
        val MEDIA_FOLDERS = stringPreferencesKey("media_folder_uris")
        val MEDIA_CONTENT_FILTER = stringPreferencesKey("media_content_filter")
        val MUSIC_SOURCE = stringPreferencesKey("music_source_type")
        val MUSIC_FOLDER = stringPreferencesKey("music_folder_uri")
        val MUSIC_THEME = stringPreferencesKey("music_theme")
        val SPOTIFY_URL = stringPreferencesKey("spotify_playlist_url")
        val YOUTUBE_URL = stringPreferencesKey("youtube_playlist_url")
        val MUSIC_SHUFFLE = booleanPreferencesKey("music_shuffle")
        val VIDEO_MUSIC_BEHAVIOR = stringPreferencesKey("video_music_behavior")
        val DUCKED_VOLUME = floatPreferencesKey("ducked_music_volume")
        val PLAYBACK_SHOW_CLOCK = booleanPreferencesKey("playback_show_clock")
        val PLAYBACK_SHOW_OVERLAY = booleanPreferencesKey("playback_show_overlay")
        val PLAYBACK_IMMERSIVE = booleanPreferencesKey("playback_immersive")
        val TRANSITION_DURATION_MS = intPreferencesKey("transition_duration_ms")
        val SHOW_SCREEN_BORDER = booleanPreferencesKey("show_screen_border")
        val UI_SCALE = floatPreferencesKey("ui_scale")
    }

    override fun observeConfig(): Flow<SlideshowConfig> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs.toConfig() }

    override suspend fun getConfig(): SlideshowConfig =
        context.dataStore.data.catch { emit(emptyPreferences()) }.first().toConfig()

    override suspend fun saveConfig(config: SlideshowConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INTERVAL] = config.intervalSeconds
            prefs[Keys.TRANSITION] = config.transition.name
            prefs[Keys.SHUFFLE] = config.shuffle
            prefs[Keys.LOOP] = config.loop
            prefs[Keys.SHOW_CLOCK] = config.showClock
            prefs[Keys.SHOW_DATE] = config.showDate
            prefs[Keys.CLOCK_POSITION] = config.clockPosition.name
            prefs[Keys.MUSIC_VOLUME] = config.musicVolume
            prefs[Keys.MEDIA_VOLUME] = config.mediaVolume
            prefs[Keys.MUTE_VIDEO] = config.muteVideoAudio
            prefs[Keys.VIDEO_PLAY_FULL] = config.videoPlayFull
            prefs[Keys.BRIGHTNESS] = config.brightness
            prefs[Keys.SELECTED_ALBUMS] = config.selectedAlbumIds.joinToString("|")
            prefs[Keys.MUSIC_PLAYLIST] = config.musicPlaylistId ?: ""
            prefs[Keys.AUTO_START] = config.autoStartOnBoot
            prefs[Keys.SCREENSAVER] = config.screenSaverMode
            prefs[Keys.MEDIA_FOLDERS] = config.mediaFolderUris.joinToString("|")
            prefs[Keys.MEDIA_CONTENT_FILTER] = config.mediaContentFilter.name
            prefs[Keys.MUSIC_SOURCE] = config.musicSourceType.name
            prefs[Keys.MUSIC_FOLDER] = config.musicFolderUri ?: ""
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
        }
        syncBootPreference(config.autoStartOnBoot)
    }

    override suspend fun updateInterval(seconds: Int) {
        context.dataStore.edit { it[Keys.INTERVAL] = seconds }
    }

    override suspend fun updateTransition(type: TransitionType) {
        context.dataStore.edit { it[Keys.TRANSITION] = type.name }
    }

    override suspend fun updateMusicVolume(volume: Float) {
        context.dataStore.edit { it[Keys.MUSIC_VOLUME] = volume }
    }

    override suspend fun toggleShuffle(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE] = enabled }
    }

    override suspend fun toggleClock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_CLOCK] = enabled }
    }

    override suspend fun updateSelectedAlbums(albumIds: List<String>) {
        context.dataStore.edit { it[Keys.SELECTED_ALBUMS] = albumIds.joinToString("|") }
    }

    private fun syncBootPreference(autoStart: Boolean) {
        context.getSharedPreferences("settings_cache", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("auto_start_boot", autoStart)
            .apply()
    }

    private fun Preferences.toConfig() = SlideshowConfig(
        intervalSeconds = this[Keys.INTERVAL] ?: 8,
        transition = enumOrDefault(this[Keys.TRANSITION], TransitionType.CROSSFADE),
        shuffle = this[Keys.SHUFFLE] ?: true,
        loop = this[Keys.LOOP] ?: true,
        showClock = this[Keys.SHOW_CLOCK] ?: true,
        showDate = this[Keys.SHOW_DATE] ?: true,
        clockPosition = enumOrDefault(this[Keys.CLOCK_POSITION], ClockPosition.BOTTOM_RIGHT),
        musicVolume = this[Keys.MUSIC_VOLUME] ?: 0.4f,
        mediaVolume = this[Keys.MEDIA_VOLUME] ?: 1.0f,
        muteVideoAudio = this[Keys.MUTE_VIDEO] ?: false,
        videoPlayFull = this[Keys.VIDEO_PLAY_FULL] ?: true,
        brightness = this[Keys.BRIGHTNESS] ?: 1.0f,
        selectedAlbumIds = splitList(this[Keys.SELECTED_ALBUMS]),
        musicPlaylistId = this[Keys.MUSIC_PLAYLIST]?.ifBlank { null },
        autoStartOnBoot = this[Keys.AUTO_START] ?: false,
        screenSaverMode = this[Keys.SCREENSAVER] ?: false,
        mediaFolderUris = splitList(this[Keys.MEDIA_FOLDERS]),
        mediaContentFilter = enumOrDefault(this[Keys.MEDIA_CONTENT_FILTER], MediaContentFilter.ALL),
        musicSourceType = enumOrDefault(this[Keys.MUSIC_SOURCE], MusicSourceType.DEVICE_LIBRARY),
        musicFolderUri = this[Keys.MUSIC_FOLDER]?.ifBlank { null },
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
        uiScale = this[Keys.UI_SCALE] ?: 1.0f
    )

    private fun splitList(raw: String?): List<String> =
        raw?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: return default) }.getOrDefault(default)
}
