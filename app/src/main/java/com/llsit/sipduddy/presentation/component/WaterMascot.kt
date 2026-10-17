package com.llsit.sipduddy.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.llsit.sipduddy.ui.theme.Ink
import kotlin.math.sin

@Composable
internal fun WaterMascot(fraction: Float, modifier: Modifier = Modifier, waveEnabled: Boolean = true) {
    val fill = animateFloatAsState(fraction, tween(1000), label = "water level")
    // Dispose the infinite transition while the mascot is covered or its page is offscreen.
    val phase: State<Float> = if (waveEnabled) {
        val transition = rememberInfiniteTransition(label = "wave")
        transition.animateFloat(
            0f, 6.283f,
            infiniteRepeatable(tween(3000, easing = LinearEasing)),
            label = "phase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(modifier
        .semantics {
            contentDescription = "บัดดี้หยดน้ำ เติมแล้ว ${(fraction * 100).toInt()} เปอร์เซ็นต์"
        }
        .drawWithCache {
            val w = size.width
            val h = size.height
            val drop = Path().apply {
                moveTo(w * .5f, h * .06f)
                cubicTo(w * .39f, h * .27f, w * .16f, h * .4f, w * .16f, h * .62f)
                cubicTo(w * .16f, h * 1.02f, w * .84f, h * 1.02f, w * .84f, h * .62f)
                cubicTo(w * .84f, h * .4f, w * .61f, h * .27f, w * .5f, h * .06f)
                close()
            }
            val water = Path()
            val outline = Stroke(2.dp.toPx())
            val smile = Stroke(2.5.dp.toPx())
            onDrawBehind {
                drawOval(Color(0xFFE9E3D7), Offset(w * .22f, h * .9f), Size(w * .56f, h * .06f))
                drawPath(drop, Color(0xFFDFF3FA))
                clipPath(drop) {
                    water.rewind()
                    val level = h * (.92f - .86f * fill.value)
                    water.moveTo(0f, h)
                    water.lineTo(0f, level)
                    val currentPhase = phase.value
                    for (x in 0..100) water.lineTo(
                        w * x / 100,
                        level + sin(x / 100f * 6.283f + currentPhase) * h * .016f
                    )
                    water.lineTo(w, h)
                    water.close()
                    drawPath(water, Color(0xFF78C9E8))
                    drawCircle(Color.White.copy(alpha = .5f), w * .035f, Offset(w * .3f, h * .75f))
                    drawCircle(Color.White.copy(alpha = .4f), w * .02f, Offset(w * .7f, h * .83f))
                }
                drawPath(drop, Color(0xFF4E9EBB), style = outline)
                drawOval(
                    Color.White.copy(alpha = .85f),
                    Offset(w * .28f, h * .38f),
                    Size(w * .065f, h * .14f)
                )
                drawCircle(Ink, w * .022f, Offset(w * .41f, h * .62f))
                drawCircle(Ink, w * .022f, Offset(w * .59f, h * .62f))
                drawOval(Color(0xFFF3B7AE), Offset(w * .31f, h * .65f), Size(w * .09f, h * .04f))
                drawOval(Color(0xFFF3B7AE), Offset(w * .60f, h * .65f), Size(w * .09f, h * .04f))
                drawArc(
                    Ink,
                    0f,
                    180f,
                    false,
                    Offset(w * .455f, h * .635f),
                    Size(w * .09f, h * .065f),
                    style = smile
                )
            }
        })
}
