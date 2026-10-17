package com.llsit.sipduddy.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.llsit.sipduddy.presentation.animation.SipBuddyMotion

@Composable
internal fun AnimatedSelectionChip(
    selected: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val scale = animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = tween(SipBuddyMotion.CONTENT_MILLIS),
        label = "selection scale",
    )
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        label = { Text(label) },
    )
}
