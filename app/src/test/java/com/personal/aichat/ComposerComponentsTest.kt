package com.personal.aichat

import com.personal.aichat.ui.ComposerPrimaryAction
import com.personal.aichat.ui.ComposerTool
import com.personal.aichat.ui.availableComposerTools
import com.personal.aichat.ui.resolveComposerPrimaryAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerComponentsTest {
  @Test
  fun primaryActionFollowsGenerationSendPanelAndToolPriority() {
    assertEquals(
      ComposerPrimaryAction.STOP,
      action(text = "message", attachments = true, generating = true, panelOpen = true, hasTools = true)
    )
    assertEquals(
      ComposerPrimaryAction.SEND,
      action(text = "message", attachments = false, panelOpen = true, hasTools = true)
    )
    assertEquals(
      ComposerPrimaryAction.SEND,
      action(text = "", attachments = true, panelOpen = true, hasTools = true)
    )
    assertEquals(
      ComposerPrimaryAction.CLOSE_PANEL,
      action(text = "", attachments = false, panelOpen = true, hasTools = true)
    )
    assertEquals(
      ComposerPrimaryAction.ADD,
      action(text = "", attachments = false, panelOpen = false, hasTools = true)
    )
    assertEquals(
      ComposerPrimaryAction.NONE,
      action(text = "", attachments = false, panelOpen = false, hasTools = false)
    )
  }

  @Test
  fun ordinaryResponsesChatShowsAttachmentsRetryAndInlineImageTools() {
    assertEquals(
      listOf(
        ComposerTool.GALLERY,
        ComposerTool.CAMERA,
        ComposerTool.FILE,
        ComposerTool.RETRY,
        ComposerTool.INLINE_IMAGES
      ),
      availableComposerTools(
        attachmentsEnabled = true,
        imageMode = false,
        inlineImagesAvailable = true,
        showRetry = true
      )
    )
  }

  @Test
  fun textOnlyProviderKeepsOnlyAvailableRetryTool() {
    assertEquals(
      listOf(ComposerTool.RETRY),
      availableComposerTools(
        attachmentsEnabled = false,
        imageMode = false,
        inlineImagesAvailable = false,
        showRetry = true
      )
    )
  }

  @Test
  fun groupComposerShowsOnlyAttachmentTools() {
    assertEquals(
      listOf(ComposerTool.GALLERY, ComposerTool.CAMERA, ComposerTool.FILE),
      availableComposerTools(
        attachmentsEnabled = true,
        imageMode = false,
        inlineImagesAvailable = false,
        showRetry = false
      )
    )
  }

  @Test
  fun imageConversationShowsImageInputsAndAllGenerationSettings() {
    val tools = availableComposerTools(
      attachmentsEnabled = true,
      imageMode = true,
      inlineImagesAvailable = true,
      showRetry = true
    )

    assertEquals(
      listOf(
        ComposerTool.GALLERY,
        ComposerTool.CAMERA,
        ComposerTool.IMAGE_SIZE,
        ComposerTool.IMAGE_QUALITY,
        ComposerTool.IMAGE_COUNT,
        ComposerTool.IMAGE_FORMAT,
        ComposerTool.IMAGE_BACKGROUND
      ),
      tools
    )
  }

  @Test
  fun composerWithoutCapabilitiesDoesNotExposeEmptyPanel() {
    assertEquals(
      emptyList<ComposerTool>(),
      availableComposerTools(
        attachmentsEnabled = false,
        imageMode = false,
        inlineImagesAvailable = false,
        showRetry = false
      )
    )
  }

  private fun action(
    text: String,
    attachments: Boolean,
    generating: Boolean = false,
    panelOpen: Boolean,
    hasTools: Boolean
  ): ComposerPrimaryAction = resolveComposerPrimaryAction(
    text = text,
    hasAttachments = attachments,
    isGenerating = generating,
    panelOpen = panelOpen,
    hasTools = hasTools
  )
}
