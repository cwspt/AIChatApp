package com.personal.aichat.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun MessageScrollIndicator(
  listState: LazyListState,
  onDragProgress: (Float) -> Unit,
  modifier: Modifier = Modifier,
  visible: Boolean = true
) {
  if (!visible) return
  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
  val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
  val metrics by remember(listState) {
    derivedStateOf { listState.scrollbarMetrics() }
  }
  Box(
    modifier = modifier
      .width(5.dp)
      .fillMaxHeight(0.82f)
      .pointerInput(Unit) {
        detectDragGestures { change, _ ->
          val y = change.position.y.coerceIn(0f, size.height.toFloat())
          onDragProgress((y / size.height.toFloat()).coerceIn(0f, 1f))
        }
      }
      .drawWithContent {
        drawContent()
        val currentMetrics = metrics ?: return@drawWithContent
        val radius = size.width / 2f
        val thumbHeightPx = 38.dp.toPx().coerceAtMost(size.height)
        val travel = (size.height - thumbHeightPx).coerceAtLeast(0f)
        val thumbTop = travel * currentMetrics.progress
        drawRoundRect(
          color = trackColor,
          topLeft = Offset.Zero,
          size = Size(size.width, size.height),
          cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
          color = thumbColor,
          topLeft = Offset(x = 1.dp.toPx(), y = thumbTop),
          size = Size((size.width - 2.dp.toPx()).coerceAtLeast(1f), thumbHeightPx),
          cornerRadius = CornerRadius(radius, radius)
        )
      }
  )
}

private data class ScrollbarMetrics(val progress: Float)

private fun LazyListState.scrollbarMetrics(): ScrollbarMetrics? {
  if (!canScroll()) return null
  return ScrollbarMetrics(scrollProgress())
}

private fun LazyListState.scrollProgress(): Float {
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return 0f
  val first = visibleItems.first()
  val visibleCount = visibleItems.size.coerceAtLeast(1)
  val totalItems = layoutInfo.totalItemsCount.coerceAtLeast(visibleCount)
  val itemOffsetFraction = ((layoutInfo.viewportStartOffset - first.offset).toFloat() / first.size.coerceAtLeast(1))
    .coerceIn(0f, 1f)
  return ((first.index + itemOffsetFraction) / (totalItems - visibleCount + 1).coerceAtLeast(1)).coerceIn(0f, 1f)
}

internal fun LazyListState.itemIndexForProgress(progress: Float): Int {
  val total = layoutInfo.totalItemsCount
  if (total <= 0) return 0
  return (progress.coerceIn(0f, 1f) * (total - 1)).roundToInt()
}

internal fun LazyListState.shouldShowScrollToBottom(extraItemThreshold: Int = 1): Boolean {
  val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
  val lastItem = layoutInfo.totalItemsCount - 1
  return lastItem > 0 && lastVisible < lastItem - extraItemThreshold
}

private fun LazyListState.canScroll(): Boolean {
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return false
  val first = visibleItems.first()
  val last = visibleItems.last()
  return first.index > 0 ||
    first.offset < layoutInfo.viewportStartOffset ||
    last.index < layoutInfo.totalItemsCount - 1 ||
    last.offset + last.size > layoutInfo.viewportEndOffset
}

internal fun LazyListState.isAtBottom(): Boolean {
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return true
  val last = visibleItems.last()
  return last.index >= layoutInfo.totalItemsCount - 1 &&
    last.offset + last.size <= layoutInfo.viewportEndOffset
}
