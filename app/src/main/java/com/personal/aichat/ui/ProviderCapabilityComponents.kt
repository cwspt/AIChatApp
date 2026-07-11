package com.personal.aichat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ProviderType

@Composable
internal fun ProviderCapabilityMatrix(providers: List<ChatProviderConfig>) {
  var expanded by remember { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("能力矩阵", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(
          text = "${providers.size} 个配置",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
          onClick = { expanded = !expanded },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = if (expanded) "收起能力矩阵" else "展开能力矩阵"
          )
        }
      }
      if (expanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
        ) {
          Row(
            modifier = Modifier
              .defaultMinSize(minWidth = 560.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("API 配置", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            ProviderCapabilityHeader("图片")
            ProviderCapabilityHeader("文件")
            ProviderCapabilityHeader("搜索")
            ProviderCapabilityHeader("Reasoning")
          }
          providers.forEach { provider ->
            Row(
              modifier = Modifier
                .defaultMinSize(minWidth = 560.dp)
                .padding(top = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(provider.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(provider.defaultModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              ProviderCapabilityCell(provider.supportsImageInput())
              ProviderCapabilityCell(provider.supportsFileInput())
              ProviderCapabilityCell(provider.supportsWebSearchTools())
              ProviderCapabilityCell(provider.supportsReasoningCapability())
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProviderCapabilityHeader(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center,
    modifier = Modifier.width(82.dp)
  )
}

@Composable
private fun ProviderCapabilityCell(supported: Boolean) {
  Row(
    modifier = Modifier.width(82.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = if (supported) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
      contentColor = if (supported) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          if (supported) Icons.Outlined.CheckCircle else Icons.Outlined.Close,
          contentDescription = null,
          modifier = Modifier.size(14.dp)
        )
        Text(if (supported) "支持" else "否", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
internal fun ProviderCapabilityBadges(provider: ChatProviderConfig) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(top = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    ProviderCapabilityBadge("图", provider.supportsImageInput())
    ProviderCapabilityBadge("文件", provider.supportsFileInput())
    ProviderCapabilityBadge("搜索", provider.supportsWebSearchTools())
    ProviderCapabilityBadge("推理", provider.supportsReasoningCapability())
  }
}

@Composable
private fun ProviderCapabilityBadge(label: String, supported: Boolean) {
  Surface(
    color = if (supported) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    contentColor = if (supported) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(8.dp)
  ) {
    Text(
      text = if (supported) label else "$label -",
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
    )
  }
}

private fun ChatProviderConfig.supportsImageInput(): Boolean = supportsAttachments

private fun ChatProviderConfig.supportsFileInput(): Boolean = supportsAttachments

private fun ChatProviderConfig.supportsWebSearchTools(): Boolean {
  return when (type) {
    ProviderType.OPENAI_RESPONSES,
    ProviderType.OPENAI_COMPATIBLE_CHAT,
    ProviderType.TOKENHUB_PROXY -> true
    ProviderType.ANTHROPIC_MESSAGES,
    ProviderType.GEMINI_GENERATE_CONTENT -> false
  }
}

private fun ChatProviderConfig.supportsReasoningCapability(): Boolean {
  return when (type) {
    ProviderType.OPENAI_RESPONSES,
    ProviderType.OPENAI_COMPATIBLE_CHAT,
    ProviderType.TOKENHUB_PROXY -> true
    ProviderType.ANTHROPIC_MESSAGES,
    ProviderType.GEMINI_GENERATE_CONTENT -> false
  }
}
