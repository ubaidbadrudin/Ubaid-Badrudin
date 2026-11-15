package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GameSfx(@RawRes val resId: Int) {
  CHOICE_SELECT(R.raw.sfx_choice),
  ITEM_COLLECTED(R.raw.sfx_item_found),
  LEVEL_UP(R.raw.sfx_level_up),
}

data class AudioSettingsState(
  val bgmEnabled: Boolean = true,
  val sfxEnabled: Boolean = true,
  val bgmVolume: Float = 0.45f,
  val sfxVolume: Float = 0.85f,
)

interface SoundController {
  val settings: StateFlow<AudioSettingsState>

  fun startBgm()

  fun pauseBgm()

  fun resumeBgm()

  fun stopAndRelease()

  fun playSfx(effect: GameSfx)

  fun setBgmEnabled(enabled: Boolean)

  fun setSfxEnabled(enabled: Boolean)

  fun toggleBgm()

  fun toggleSfx()

  fun setBgmVolume(volume: Float)

  fun setSfxVolume(volume: Float)
}

/**
 * Implementasi aman tanpa perangkat audio untuk pengujian unit JVM & preview Compose.
 */
object NoOpSoundController : SoundController {
  private val _settings = MutableStateFlow(AudioSettingsState())
  override val settings: StateFlow<AudioSettingsState> = _settings.asStateFlow()

  override fun startBgm() = Unit

  override fun pauseBgm() = Unit

  override fun resumeBgm() = Unit

  override fun stopAndRelease() = Unit

  override fun playSfx(effect: GameSfx) = Unit

  override fun setBgmEnabled(enabled: Boolean) {
    _settings.update { it.copy(bgmEnabled = enabled) }
  }

  override fun setSfxEnabled(enabled: Boolean) {
    _settings.update { it.copy(sfxEnabled = enabled) }
  }

  override fun toggleBgm() {
    _settings.update { it.copy(bgmEnabled = !it.bgmEnabled) }
  }

  override fun toggleSfx() {
    _settings.update { it.copy(sfxEnabled = !it.sfxEnabled) }
  }

  override fun setBgmVolume(volume: Float) {
    _settings.update { it.copy(bgmVolume = volume.coerceIn(0f, 1f)) }
  }

  override fun setSfxVolume(volume: Float) {
    _settings.update { it.copy(sfxVolume = volume.coerceIn(0f, 1f)) }
  }
}

/**
 * Pengelola audio game sederhana:
 * - Memutar musik latar (BGM) secara berulang menggunakan [MediaPlayer].
 * - Memutar efek suara latensi rendah (pilihan aksi, perolehan item, naik level) menggunakan [SoundPool].
 * - Menyimpan preferensi suara pemain ke [SharedPreferences].
 */
class GameSoundManager(context: Context) : SoundController {
  private val appContext = context.applicationContext
  private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _settings =
    MutableStateFlow(
      AudioSettingsState(
        bgmEnabled = prefs.getBoolean(KEY_BGM_ENABLED, true),
        sfxEnabled = prefs.getBoolean(KEY_SFX_ENABLED, true),
        bgmVolume = prefs.getFloat(KEY_BGM_VOLUME, 0.45f).coerceIn(0f, 1f),
        sfxVolume = prefs.getFloat(KEY_SFX_VOLUME, 0.85f).coerceIn(0f, 1f),
      )
    )
  override val settings: StateFlow<AudioSettingsState> = _settings.asStateFlow()

  private val sfxLoaded = ConcurrentHashMap<Int, Boolean>()
  private val sfxSampleIds = mutableMapOf<GameSfx, Int>()

  private val soundPool: SoundPool? =
    runCatching {
        val attrs =
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
          .setMaxStreams(MAX_SFX_STREAMS)
          .setAudioAttributes(attrs)
          .build()
          .apply {
            setOnLoadCompleteListener { _, sampleId, status ->
              if (status == 0) {
                sfxLoaded[sampleId] = true
              }
            }
            GameSfx.entries.forEach { effect ->
              val id = load(appContext, effect.resId, 1)
              sfxSampleIds[effect] = id
            }
          }
      }
      .getOrNull()

  private var bgmPlayer: MediaPlayer? = null
  private var isForeground: Boolean = false
  private var isReleased: Boolean = false

  override fun startBgm() {
    isForeground = true
    if (!isReleased && _settings.value.bgmEnabled) {
      ensureAndStartBgm()
    }
  }

  override fun resumeBgm() {
    isForeground = true
    if (!isReleased && _settings.value.bgmEnabled) {
      ensureAndStartBgm()
    }
  }

  override fun pauseBgm() {
    isForeground = false
    runCatching {
      if (bgmPlayer?.isPlaying == true) {
        bgmPlayer?.pause()
      }
    }
  }

  override fun stopAndRelease() {
    if (isReleased) return
    isReleased = true
    isForeground = false

    runCatching {
      bgmPlayer?.apply {
        if (isPlaying) stop()
        release()
      }
    }
    bgmPlayer = null

    runCatching { soundPool?.release() }
    sfxSampleIds.clear()
    sfxLoaded.clear()
  }

  override fun playSfx(effect: GameSfx) {
    val current = _settings.value
    if (isReleased || !current.sfxEnabled) return
    val pool = soundPool ?: return
    val sampleId = sfxSampleIds[effect] ?: return
    val vol = current.sfxVolume.coerceIn(0f, 1f)
    if (vol <= 0f) return

    if (sfxLoaded[sampleId] == true) {
      runCatching { pool.play(sampleId, vol, vol, 1, 0, 1f) }
    } else {
      // Coba putar jika load selesai seketika, abaikan secara aman bila masih memuat.
      runCatching { pool.play(sampleId, vol, vol, 1, 0, 1f) }
    }
  }

  override fun setBgmEnabled(enabled: Boolean) {
    _settings.update { it.copy(bgmEnabled = enabled) }
    prefs.edit().putBoolean(KEY_BGM_ENABLED, enabled).apply()
    if (enabled && isForeground && !isReleased) {
      ensureAndStartBgm()
    } else {
      runCatching {
        if (bgmPlayer?.isPlaying == true) {
          bgmPlayer?.pause()
        }
      }
    }
  }

  override fun setSfxEnabled(enabled: Boolean) {
    _settings.update { it.copy(sfxEnabled = enabled) }
    prefs.edit().putBoolean(KEY_SFX_ENABLED, enabled).apply()
  }

  override fun toggleBgm() {
    setBgmEnabled(!_settings.value.bgmEnabled)
  }

  override fun toggleSfx() {
    setSfxEnabled(!_settings.value.sfxEnabled)
  }

  override fun setBgmVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    _settings.update { it.copy(bgmVolume = clamped) }
    prefs.edit().putFloat(KEY_BGM_VOLUME, clamped).apply()
    runCatching { bgmPlayer?.setVolume(clamped, clamped) }
  }

  override fun setSfxVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    _settings.update { it.copy(sfxVolume = clamped) }
    prefs.edit().putFloat(KEY_SFX_VOLUME, clamped).apply()
  }

  private fun ensureAndStartBgm() {
    if (isReleased || !_settings.value.bgmEnabled) return
    val player = bgmPlayer ?: createBgmPlayer()?.also { bgmPlayer = it } ?: return

    val vol = _settings.value.bgmVolume.coerceIn(0f, 1f)
    runCatching {
      player.setVolume(vol, vol)
      if (!player.isPlaying) {
        player.start()
      }
    }
  }

  private fun createBgmPlayer(): MediaPlayer? {
    val attrs =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    val preparedDirect =
      runCatching {
        val afd = appContext.resources.openRawResourceFd(R.raw.bgm_adventure) ?: return@runCatching null
        val player = MediaPlayer()
        try {
          afd.use { descriptor ->
            player.setAudioAttributes(attrs)
            player.setDataSource(
              descriptor.fileDescriptor,
              descriptor.startOffset,
              descriptor.length,
            )
            player.isLooping = true
            player.prepare()
          }
          player
        } catch (t: Throwable) {
          runCatching { player.release() }
          throw t
        }
      }.getOrNull()

    if (preparedDirect != null) return preparedDirect

    return runCatching {
      MediaPlayer.create(appContext, R.raw.bgm_adventure)?.apply {
        isLooping = true
      }
    }.getOrNull()
  }

  companion object {
    private const val PREFS_NAME = "loka_audio_settings"
    private const val KEY_BGM_ENABLED = "bgm_enabled"
    private const val KEY_SFX_ENABLED = "sfx_enabled"
    private const val KEY_BGM_VOLUME = "bgm_volume"
    private const val KEY_SFX_VOLUME = "sfx_volume"
    private const val MAX_SFX_STREAMS = 4
  }
}
