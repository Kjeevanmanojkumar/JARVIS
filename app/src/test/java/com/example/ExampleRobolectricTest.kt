package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiConfig
import com.example.automation.PhoneAutomationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS", appName)
  }

  @Test
  fun `verify system instructions for personality modes`() {
    val normalPrompt = GeminiConfig.getSystemInstruction(GeminiConfig.PersonalityMode.NORMAL)
    assertTrue(normalPrompt.contains("JARVIS"))

    val seriousPrompt = GeminiConfig.getSystemInstruction(GeminiConfig.PersonalityMode.SERIOUS)
    assertTrue(seriousPrompt.contains("Tactical"))

    val companionPrompt = GeminiConfig.getSystemInstruction(GeminiConfig.PersonalityMode.COMPANION)
    assertTrue(companionPrompt.contains("Tony Stark"))
  }

  @Test
  fun `verify phone automation manager instantiation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = PhoneAutomationManager(context)
    assertNotNull(manager)
  }
}
