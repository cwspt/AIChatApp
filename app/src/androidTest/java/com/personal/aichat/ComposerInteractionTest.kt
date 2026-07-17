package com.personal.aichat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.ui.Composer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposerInteractionTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun emptyComposerOpensToolsAndTypingSwitchesToSend() {
    val input = mutableStateOf(TextFieldValue(""))
    composeRule.setContent {
      MaterialTheme {
        Composer(
          input = input.value,
          attachments = emptyList(),
          attachmentsEnabled = true,
          inlineImagesAvailable = true,
          onInput = { input.value = it },
          onSend = {},
          onRetry = {},
          onPickImages = {},
          onPickFiles = {},
          onTakePhoto = {},
          onRemoveAttachment = {},
          onOpenAttachment = {},
          isGenerating = false,
          onStopGenerating = {}
        )
      }
    }

    composeRule.onNodeWithContentDescription("展开更多工具").performClick()
    composeRule.onNodeWithText("相册").assertIsDisplayed()
    composeRule.onNodeWithText("回复插图").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("收起更多工具").assertIsDisplayed()

    composeRule.onNode(hasSetTextAction()).performClick().performTextInput("hello")

    composeRule.onNodeWithContentDescription("发送").assertIsDisplayed()
    composeRule.onNodeWithText("相册").assertDoesNotExist()
  }

  @Test
  fun imageSettingsSummaryOpensGenerationToolPanel() {
    composeRule.setContent {
      MaterialTheme {
        Composer(
          input = TextFieldValue(""),
          attachments = emptyList(),
          attachmentsEnabled = true,
          imageMode = true,
          imageOptions = ImageGenerationOptions(),
          onInput = {},
          onSend = {},
          onRetry = {},
          onPickImages = {},
          onPickFiles = {},
          onTakePhoto = {},
          onRemoveAttachment = {},
          onOpenAttachment = {},
          isGenerating = false,
          onStopGenerating = {}
        )
      }
    }

    composeRule.onNodeWithText("自动 · 自动质量 · 1 张 · PNG · 自动背景").performClick()

    composeRule.onNodeWithText("尺寸").assertIsDisplayed()
    composeRule.onNodeWithText("质量").assertIsDisplayed()
    composeRule.onNodeWithText("数量").assertIsDisplayed()
    composeRule.onNodeWithText("格式").assertIsDisplayed()
    composeRule.onNodeWithText("背景").assertIsDisplayed()
    composeRule.onNodeWithText("文件").assertDoesNotExist()
  }
}
