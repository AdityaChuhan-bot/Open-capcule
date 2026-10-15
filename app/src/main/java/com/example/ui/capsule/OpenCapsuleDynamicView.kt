package com.example.ui.capsule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AnimationSpeed
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import com.example.model.CapsuleThemeMode
import com.example.ui.theme.CapsuleDarkBg
import com.example.ui.theme.CapsulePillBlack
import com.example.ui.theme.PrimaryBlue

/**
 * The core OpenCapsule dynamic presentation component.
 * Renders both collapsed status bar pill and expanded interactive widget with spring physics.
 */
@Composable
fun OpenCapsuleDynamicView(
  event: CapsuleEvent?,
  isExpanded: Boolean,
  config: CapsuleConfig,
  onToggleExpand: () -> Unit,
  onCollapse: () -> Unit,
  modifier: Modifier = Modifier
) {
  val springSpec = when (config.animationSpeed) {
    AnimationSpeed.SLOW -> spring<androidx.compose.ui.unit.Dp>(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessVeryLow
    )
    AnimationSpeed.NORMAL -> spring<androidx.compose.ui.unit.Dp>(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    )
    AnimationSpeed.FAST -> spring<androidx.compose.ui.unit.Dp>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMedium
    )
    AnimationSpeed.SPRINGY -> spring<androidx.compose.ui.unit.Dp>(
      dampingRatio = Spring.DampingRatioHighBouncy,
      stiffness = Spring.StiffnessLow
    )
  }

  val targetWidth = if (isExpanded) config.expandedWidthDp.dp else config.collapsedWidthDp.dp
  val targetHeight = if (isExpanded) config.expandedHeightDp.dp else config.collapsedHeightDp.dp
  val targetCornerRadius = if (isExpanded) 28.dp else config.cornerRadiusDp.dp

  val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = springSpec, label = "capsule_width")
  val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = springSpec, label = "capsule_height")
  val animatedRadius by animateDpAsState(targetValue = targetCornerRadius, animationSpec = springSpec, label = "capsule_radius")

  val backgroundColor = when (config.themeMode) {
    CapsuleThemeMode.SYSTEM -> if (MaterialTheme.colorScheme.surface.hashCode() % 2 == 0) CapsulePillBlack else CapsuleDarkBg
    CapsuleThemeMode.DARK -> CapsulePillBlack
    CapsuleThemeMode.LIGHT -> MaterialTheme.colorScheme.surfaceContainerHighest
    CapsuleThemeMode.DYNAMIC_MATERIAL_YOU -> CapsulePillBlack
  }

  val shape = RoundedCornerShape(animatedRadius)
  val interactionSource = remember { MutableInteractionSource() }

  Surface(
    modifier = modifier
      .width(animatedWidth)
      .height(animatedHeight)
      .shadow(elevation = if (isExpanded) 16.dp else 8.dp, shape = shape)
      .clip(shape)
      .border(
        width = 1.dp,
        color = if (isExpanded) PrimaryBlue.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        shape = shape
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = !isExpanded,
        onClick = onToggleExpand
      )
      .pointerInput(isExpanded) {
        if (isExpanded) {
          detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount < -12f) {
              onCollapse()
            }
          }
        }
      }
      .testTag("open_capsule_main_view"),
    color = backgroundColor,
    shape = shape
  ) {
    Box(
      modifier = Modifier
        .padding(horizontal = if (isExpanded) 16.dp else 10.dp, vertical = if (isExpanded) 12.dp else 4.dp),
      contentAlignment = Alignment.Center
    ) {
      AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
          fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh))
        },
        label = "capsule_content_transition"
      ) { expanded ->
        if (expanded) {
          ExpandedCapsuleContent(event = event, config = config, onCollapse = onCollapse)
        } else {
          CollapsedCapsuleContent(event = event, config = config)
        }
      }
    }
  }
}
