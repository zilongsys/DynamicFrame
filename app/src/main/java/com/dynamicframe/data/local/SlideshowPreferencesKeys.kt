package com.dynamicframe.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Claves únicas de DataStore para configuración del slideshow. */
object SlideshowPreferencesKeys {
    const val DATASTORE_FILE = "slideshow_settings"
    val INTERVAL = intPreferencesKey("interval_seconds")
    val TRANSITION = stringPreferencesKey("transition_type")
    val SHUFFLE = booleanPreferencesKey("shuffle")
    val PHOTO_SHUFFLE = booleanPreferencesKey("photo_shuffle")
    val VIDEO_SHUFFLE = booleanPreferencesKey("video_shuffle")
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
    val PHOTO_FOLDERS = stringPreferencesKey("photo_folder_uris")
    val VIDEO_FOLDERS = stringPreferencesKey("video_folder_uris")
    val MEDIA_CONTENT_FILTER = stringPreferencesKey("media_content_filter")
    val DISABLED_PHOTO_FOLDERS = stringPreferencesKey("disabled_photo_folder_uris")
    val DISABLED_VIDEO_FOLDERS = stringPreferencesKey("disabled_video_folder_uris")
    val DISABLED_MUSIC_FOLDERS = stringPreferencesKey("disabled_music_folder_uris")
    val MUSIC_SOURCES = stringPreferencesKey("music_source_types")
    val MUSIC_SOURCE = stringPreferencesKey("music_source_type")
    val MUSIC_FOLDER = stringPreferencesKey("music_folder_uri")
    val MUSIC_FOLDERS = stringPreferencesKey("music_folder_uris")
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
    val SHOW_PICTURE_FRAME = booleanPreferencesKey("show_picture_frame")
    val PLAYBACK_SAFE_BORDER = booleanPreferencesKey("playback_safe_border")
    val PLAYBACK_FRAME_SCALE = floatPreferencesKey("playback_frame_scale")
    val PLAYBACK_CONTENT_ZOOM = floatPreferencesKey("playback_content_zoom")
    val PLAYBACK_BACKGROUND = stringPreferencesKey("playback_background_type")
    val PLAYBACK_BACKGROUND_IMAGE = stringPreferencesKey("playback_background_image_uri")
    val PLAYBACK_THEME = stringPreferencesKey("playback_theme")
}

/** Caché síncrona para `BootReceiver` (sin Hilt). Sincronizada desde DataStore al guardar config. */
object SettingsBootCache {
    const val PREFS_NAME = "settings_cache"
    val AUTO_START_BOOT: String = SlideshowPreferencesKeys.AUTO_START.name
}
