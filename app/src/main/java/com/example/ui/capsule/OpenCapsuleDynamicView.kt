package com.example.ui.capsule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AnimationSpeed
import com.example.model.AnimationStyle
import com.example.model.CapsuleConfig
import com.example.model.CapsuleEvent
import com.example.model.CapsuleThemeMode
import com.example.model.ExpansionBehavior
import com.example.ui.theme.CapsuleDarkBg
import com.example.ui.theme.CapsulePillBlack
import com.example.ui.theme.PrimaryBlue

/**
 * The core OpenCapsule dynamic presentation component.
 * Renders both collapsed status bar pill and expanded interactive widget
 * with fluid, customizable Material 3 physics, styles, and gestures.
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
  // Speed parameters: duration (ms), spring stiffness, damping ratio
  val (durationMillis, stiffness, dampingRatio) = when (config.animationSpeed) {
    AnimationSpeed.SLOW -> Triple(450, Spring.StiffnessVeryLow, Spring.DampingRatioLowBouncy)
    AnimationSpeed.NORMAL -> Triple(240, Spring.StiffnessMedium, Spring.DampingRatioNoBouncy)
    AnimationSpeed.FAST -> Triple(160, Spring.StiffnessMedium, Spring.DampingRatioNoBouncy)
    AnimationSpeed.SPRINGY -> Triple(320, Spring.StiffnessLow, Spring.DampingRatioHighBouncy)
  }

  // Dimension animation spec based on speed, style, or disabled state
  val sizeAnimationSpec: AnimationSpec<Dp> = if (!config.animationsEnabled) {
    snap()
  } else {
    when (config.animationStyle) {
      AnimationStyle.SPRING -> spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
      )
      AnimationStyle.FADE -> tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
      )
      AnimationStyle.BOUNCE -> spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = stiffness
      )
      AnimationStyle.SMOOTH -> tween(
        durationMillis = durationMillis,
        easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // Material 3 Emphasized Decelerate
      )
      AnimationStyle.MINIMAL -> tween(
        durationMillis = (durationMillis * 0.65f).toInt().coerceAtLeast(80),
        easing = LinearOutSlowInEasing
      )
    }
  }

  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val maxUsableWidthDp = (screenWidthDp - 24).coerceAtLeast(1)
  val targetWidth = if (isExpanded) {
    config.expandedWidthDp.coerceAtMost(maxUsableWidthDp).dp
  } else {
    config.collapsedWidthDp.coerceAtMost(maxUsableWidthDp).dp
  }
  val targetHeight = if (isExpanded) config.expandedHeightDp.dp else config.collapsedHeightDp.dp
  val targetCornerRadius = if (isExpanded) 28.dp else config.cornerRadiusDp.dp

  val animatedWidth by animateDpAsState(
    targetValue = targetWidth,
    animationSpec = sizeAnimationSpec,
    label = "capsule_width"
  )
  val animatedHeight by animateDpAsState(
    targetValue = targetHeight,
    animationSpec = sizeAnimationSpec,
    label = "capsule_height"
  )
  val animatedRadius by animateDpAsState(
    targetValue = targetCornerRadius,
    animationSpec = sizeAnimationSpec,
    label = "capsule_radius"
  )

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
      .pointerInput(isExpanded, config.swipeUpToCollapseEnabled) {
        if (isExpanded && config.swipeUpToCollapseEnabled) {
          detectVerticalDragGestures { _, dragAmount ->
            // Upward swipe to collapse the expanded widget
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
          if (!config.animationsEnabled) {
            EnterTransition.None togetherWith ExitTransition.None
          } else {
            when (config.expansionBehavior) {
              ExpansionBehavior.DOWNWARD_EXPAND -> {
                (slideInVertically(
                  initialOffsetY = { -it / 2 },
                  animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis))) togetherWith
                  (slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(durationMillis / 2, easing = FastOutLinearInEasing)
                  ) + fadeOut(animationSpec = tween(durationMillis / 2)))
              }
              ExpansionBehavior.POP_EXPAND -> {
                (scaleIn(
                  initialScale = 0.82f,
                  animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = stiffness)
                ) + fadeIn(animationSpec = spring(stiffness = stiffness))) togetherWith
                  (scaleOut(
                    targetScale = 0.88f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh)
                  ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)))
              }
              ExpansionBehavior.CENTER_EXPAND -> {
                when (config.animationStyle) {
                  AnimationStyle.FADE -> {
                    fadeIn(animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)) togetherWith
                      fadeOut(animationSpec = tween(durationMillis / 2, easing = FastOutLinearInEasing))
                  }
                  AnimationStyle.SMOOTH -> {
                    val m3Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
                    (fadeIn(animationSpec = tween(durationMillis, easing = m3Easing)) +
                      expandIn(expandFrom = Alignment.Center, animationSpec = tween(durationMillis, easing = m3Easing))) togetherWith
                      (fadeOut(animationSpec = tween(durationMillis / 2)) +
                        shrinkOut(shrinkTowards = Alignment.Center, animationSpec = tween(durationMillis / 2)))
                  }
                  AnimationStyle.MINIMAL -> {
                    fadeIn(animationSpec = tween(durationMillis / 2)) togetherWith
                      fadeOut(animationSpec = tween(durationMillis / 3))
                  }
                  AnimationStyle.SPRING, AnimationStyle.BOUNCE -> {
                    (fadeIn(animationSpec = spring(stiffness = stiffness)) +
                      scaleIn(
                        initialScale = 0.92f,
                        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness)
                      )) togetherWith
                      (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                        scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessHigh)))
                  }
                }
              }
            }
          }
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

