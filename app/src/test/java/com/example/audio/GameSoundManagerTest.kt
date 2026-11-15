package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameSoundManagerTest {

  @Test
  fun rawAudioResources_existAndReadable() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    listOf(
      R.raw.bgm_adventure,
      R.raw.sfx_choice,
      R.raw.sfx_item_found,
      R.raw.sfx_level_up,
    ).forEach { resId ->
      context.resources.openRawResource(resId).use { stream ->
        val header = ByteArray(4)
        val read = stream.read(header)
        assertEquals(4, read)
        assertEquals("RIFF", String(header, Charsets.US_ASCII))
      }
    }
  }

  @Test
  fun soundManager_togglesAndPersistsPreferencesAndClampsVolumes() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("loka_audio_settings", Context.MODE_PRIVATE).edit().clear().commit()

    val manager = GameSoundManager(context)
    assertTrue(manager.settings.value.bgmEnabled)
    assertTrue(manager.settings.value.sfxEnabled)

    manager.startBgm()
    manager.toggleBgm()
    assertFalse(manager.settings.value.bgmEnabled)

    manager.toggleSfx()
    assertFalse(manager.settings.value.sfxEnabled)

    manager.setBgmVolume(1.5f)
    manager.setSfxVolume(-0.4f)
    assertEquals(1f, manager.settings.value.bgmVolume, 0.001f)
    assertEquals(0f, manager.settings.value.sfxVolume, 0.001f)

    manager.setSfxEnabled(true)
    manager.setSfxVolume(0.75f)
    manager.playSfx(GameSfx.CHOICE_SELECT)
    manager.playSfx(GameSfx.ITEM_COLLECTED)
    manager.pauseBgm()
    manager.resumeBgm()
    manager.stopAndRelease()

    // Recreate manager to verify preferences persisted
    val reloaded = GameSoundManager(context)
    assertFalse(reloaded.settings.value.bgmEnabled)
    assertTrue(reloaded.settings.value.sfxEnabled)
    assertEquals(1f, reloaded.settings.value.bgmVolume, 0.001f)
    assertEquals(0.75f, reloaded.settings.value.sfxVolume, 0.001f)
    assertNotNull(reloaded.settings.value)
    reloaded.stopAndRelease()
  }

  @Test
  fun noOpSoundController_updatesSettingsWithoutCrashing() {
    NoOpSoundController.setBgmEnabled(false)
    assertFalse(NoOpSoundController.settings.value.bgmEnabled)
    NoOpSoundController.toggleBgm()
    assertTrue(NoOpSoundController.settings.value.bgmEnabled)

    NoOpSoundController.setSfxVolume(0.5f)
    assertEquals(0.5f, NoOpSoundController.settings.value.sfxVolume, 0.001f)
    NoOpSoundController.playSfx(GameSfx.CHOICE_SELECT)
    NoOpSoundController.playSfx(GameSfx.ITEM_COLLECTED)
  }
}
