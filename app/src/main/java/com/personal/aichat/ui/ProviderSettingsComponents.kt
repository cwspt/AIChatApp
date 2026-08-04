package com.personal.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ImageGenerationApiMode
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import com.personal.aichat.domain.isDeepSeekV4FlashModel
import com.personal.aichat.domain.parseContextWindowTokensInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderSettingsDialog(
  provider: ChatProviderConfig,
  hasSavedApiKey: Boolean,
  onDismiss: () -> Unit,
  onSave: (ChatProviderConfig, String?) -> Unit
) {
  var displayName by remember(provider.id) { mutableStateOf(provider.displayName) }
  var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
  var model by remember(provider.id) { mutableStateOf(provider.defaultModel) }
  var contextWindowTokens by remember(provider.id) { mutableStateOf(provider.contextWindowTokensOverride?.toString().orEmpty()) }
  var reasoningEffort by remember(provider.id) { mutableStateOf(provider.reasoningEffort) }
  var supportsAttachments by remember(provider.id) { mutableStateOf(provider.supportsAttachments) }
  var supportsImageGeneration by remember(provider.id) { mutableStateOf(provider.supportsImageGeneration) }
  var imageGenerationApiMode by remember(provider.id) { mutableStateOf(provider.imageGenerationApiMode) }
  var imageGenerationModel by remember(provider.id) { mutableStateOf(provider.imageGenerationModel) }
  var apiKey by remember(provider.id) { mutableStateOf("") }
  val deepSeekV4Flash = provider.type == ProviderType.OPENAI_COMPATIBLE_CHAT && isDeepSeekV4FlashModel(model)
  val parsedContextWindowTokens = remember(contextWindowTokens) {
    parseContextWindowTokensInput(contextWindowTokens)
  }
  val contextWindowInputInvalid = contextWindowTokens.isNotBlank() && parsedContextWindowTokens == null

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 640.dp)
        .padding(18.dp)
    ) {
      Column(
        modifier = Modifier.padding(18.dp)
      ) {
        Text(
          text = "编辑 API 配置",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .defaultMinSize(minHeight = 1.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = provider.type.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("API 类型") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("显示名称") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("默认模型") },
            supportingText = {
              Text(
                if (deepSeekV4Flash) {
                  "将自动使用 DeepSeek Responses API（/responses）；该模型不支持图片/文件输入。"
                } else {
                  "DeepSeek 只有 deepseek-v4-flash 会使用 Responses API，其他模型继续使用 Chat Completions。"
                }
              )
            },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = contextWindowTokens,
            onValueChange = { value ->
              contextWindowTokens = value
                .filter { it.isDigit() || it in setOf('k', 'K', 'm', 'M', ',', '_', '.', ' ') }
                .take(16)
            },
            label = { Text("上下文上限 tokens（可选）") },
            placeholder = { Text("例如 1M、400K 或 1000000") },
            supportingText = {
              Text(
                when {
                  contextWindowInputInvalid -> "无法识别；可填写 1M、400K、128000 或 1,000,000。"
                  parsedContextWindowTokens != null -> "将保存为 ${formatTokenCount(parsedContextWindowTokens)} tokens，用于容量估算和自动压缩。"
                  else -> "留空则使用内置模型表；自定义模型建议填写。"
                }
              )
            },
            isError = contextWindowInputInvalid,
            modifier = Modifier.fillMaxWidth()
          )
          if (provider.type == ProviderType.OPENAI_RESPONSES || deepSeekV4Flash) {
            ReasoningEffortSelector(
              value = reasoningEffort,
              onValueChange = { reasoningEffort = it }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("允许图片/文件附件", fontWeight = FontWeight.Medium)
              Text(
                text = "仅在供应商 API 支持多模态输入时开启。DeepSeek 当前不支持。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = supportsAttachments && !deepSeekV4Flash,
              enabled = !deepSeekV4Flash,
              onCheckedChange = { supportsAttachments = it }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("允许生图模式", fontWeight = FontWeight.Medium)
              Text(
                text = "第一版仅建议 GPT / OpenAI Responses 配置开启。DeepSeek 当前不支持图片生成。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = supportsImageGeneration,
              enabled = provider.type == ProviderType.OPENAI_RESPONSES,
              onCheckedChange = { supportsImageGeneration = it }
            )
          }
          if (provider.type == ProviderType.OPENAI_RESPONSES) {
            ImageGenerationApiModeSelector(
              value = imageGenerationApiMode,
              enabled = supportsImageGeneration,
              onValueChange = { imageGenerationApiMode = it }
            )
            OutlinedTextField(
              value = imageGenerationModel,
              onValueChange = { imageGenerationModel = it },
              enabled = supportsImageGeneration && imageGenerationApiMode == ImageGenerationApiMode.IMAGES_API,
              label = { Text("生图模型名") },
              placeholder = { Text("例如 gpt-image-2 或 gpt-image-1") },
              supportingText = {
                Text("Images API 模式使用此模型；Responses 工具模式继续使用上方默认模型。")
              },
              modifier = Modifier.fillMaxWidth()
            )
          }
          OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key 或代理 Key") },
            placeholder = {
              Text(if (hasSavedApiKey) "已保存 Key；留空则继续使用原 Key" else "请输入 API Key")
            },
            modifier = Modifier.fillMaxWidth()
          )
          if (hasSavedApiKey) {
            Text(
              text = "当前配置已有加密保存的 Key。留空则继续使用原 Key；输入新 Key 后保存会替换。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = provider.type.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Key 会通过 Android Keystore 加密保存，不写入聊天数据库。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("取消")
          }
          Spacer(Modifier.width(8.dp))
          Button(
            enabled = !contextWindowInputInvalid,
            onClick = {
              onSave(
                provider.copy(
                  displayName = displayName.trim(),
                  baseUrl = baseUrl.trim().trimEnd('/'),
                  defaultModel = model.trim(),
                  contextWindowTokensOverride = parsedContextWindowTokens,
                  enabled = true,
                  supportsAttachments = supportsAttachments && !deepSeekV4Flash,
                  supportsImageGeneration = supportsImageGeneration && provider.type == ProviderType.OPENAI_RESPONSES,
                  imageGenerationApiMode = imageGenerationApiMode,
                  imageGenerationModel = imageGenerationModel.trim(),
                  reasoningEffort = reasoningEffort
                ),
                apiKey.takeIf { it.isNotBlank() }
              )
            }
          ) {
            Text("保存")
          }
        }
      }
    }
  }
}

@Composable
private fun ReasoningEffortSelector(
  value: ReasoningEffort,
  onValueChange: (ReasoningEffort) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxWidth()) {
    AssistChip(
      onClick = { expanded = true },
      label = { Text("推理强度：${value.label}") },
      trailingIcon = {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      ReasoningEffort.entries.forEach { effort ->
        DropdownMenuItem(
          text = {
            Column {
              Text(effort.label)
              Text(
                effort.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = {
            onValueChange(effort)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun ImageGenerationApiModeSelector(
  value: ImageGenerationApiMode,
  enabled: Boolean,
  onValueChange: (ImageGenerationApiMode) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxWidth()) {
    AssistChip(
      enabled = enabled,
      onClick = { expanded = true },
      label = { Text("生图接口：${value.label}") },
      trailingIcon = {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      ImageGenerationApiMode.entries.forEach { mode ->
        DropdownMenuItem(
          text = {
            Column {
              Text(mode.label)
              Text(
                mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = {
            onValueChange(mode)
            expanded = false
          }
        )
      }
    }
  }
}

internal val ProviderType.label: String
  get() = when (this) {
    ProviderType.OPENAI_RESPONSES -> "GPT / OpenAI Responses"
    ProviderType.OPENAI_COMPATIBLE_CHAT -> "DeepSeek / OpenAI-compatible Chat Completions"
    ProviderType.TOKENHUB_PROXY -> "TokenHub 代理 / Responses 兼容"
    ProviderType.ANTHROPIC_MESSAGES -> "Anthropic Messages"
    ProviderType.GEMINI_GENERATE_CONTENT -> "Gemini GenerateContent"
  }

private val ImageGenerationApiMode.label: String
  get() = when (this) {
    ImageGenerationApiMode.RESPONSES_TOOL -> "Responses 工具模式"
    ImageGenerationApiMode.IMAGES_API -> "Images API 模式"
  }

private val ImageGenerationApiMode.description: String
  get() = when (this) {
    ImageGenerationApiMode.RESPONSES_TOOL -> "请求 /responses + image_generation，适合官方 OpenAI Responses。"
    ImageGenerationApiMode.IMAGES_API -> "请求 /images/generations 或 /images/edits，适合只支持 generations 的中转。"
  }

private val ReasoningEffort.label: String
  get() = when (this) {
    ReasoningEffort.AUTO -> "智能"
    ReasoningEffort.LOW -> "低"
    ReasoningEffort.MEDIUM -> "中"
    ReasoningEffort.HIGH -> "高"
    ReasoningEffort.XHIGH -> "超高"
  }

private val ReasoningEffort.description: String
  get() = when (this) {
    ReasoningEffort.AUTO -> "不显式发送 effort，由模型和服务端自动决定。"
    ReasoningEffort.LOW -> "更快、成本更低，适合普通问答。"
    ReasoningEffort.MEDIUM -> "平衡速度和推理质量。"
    ReasoningEffort.HIGH -> "更深入推理，适合复杂任务。"
    ReasoningEffort.XHIGH -> "尽可能高强度推理；仅部分模型支持。"
  }

private val ProviderType.description: String
  get() = when (this) {
    ProviderType.OPENAI_RESPONSES ->
      "用于 GPT / OpenAI 官方 Responses API。Base URL 示例：https://api.openai.com/v1"
    ProviderType.OPENAI_COMPATIBLE_CHAT ->
      "用于 DeepSeek 以及其他 OpenAI-compatible 服务。模型填写 deepseek-v4-flash 时自动请求 Responses API；其他模型使用 Chat Completions。DeepSeek Base URL：https://api.deepseek.com"
    ProviderType.TOKENHUB_PROXY ->
      "用于本机或局域网中暴露 Responses-compatible /v1 API 的代理。"
    ProviderType.ANTHROPIC_MESSAGES ->
      "Reserved for a future Anthropic Messages adapter."
    ProviderType.GEMINI_GENERATE_CONTENT ->
      "Reserved for a future Gemini GenerateContent adapter."
  }
