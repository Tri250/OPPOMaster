package com.omaster.app.ui.components

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun DraggableWatermark(
    modifier: Modifier = Modifier,
    initialPosition: Offset = Offset(0.5f, 0.5f),
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    minScale: Float = 0.2f,
    maxScale: Float = 3f,
    boundaryRect: IntSize = IntSize.Zero,
    onPositionChange: (Offset) -> Unit = {},
    onScaleChange: (Float) -> Unit = {},
    onRotationChange: (Float) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onTap: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(initialScale) }
    var rotation by remember { mutableFloatStateOf(initialRotation) }
    var isDragging by remember { mutableStateOf(false) }
    var elementSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                elementSize = coordinates.size
            }
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        if (boundaryRect.width > 0 && boundaryRect.height > 0 && elementSize.width > 0) {
                            val halfWidth = elementSize.width * scale / 2
                            val halfHeight = elementSize.height * scale / 2
                            val boundaryHalfWidth = boundaryRect.width / 2
                            val boundaryHalfHeight = boundaryRect.height / 2

                            val maxOffsetX = boundaryHalfWidth - halfWidth
                            val maxOffsetY = boundaryHalfHeight - halfHeight

                            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                        }

                        val newPosition = if (boundaryRect.width > 0 && boundaryRect.height > 0) {
                            Offset(
                                x = (offsetX + boundaryRect.width / 2) / boundaryRect.width,
                                y = (offsetY + boundaryRect.height / 2) / boundaryRect.height
                            )
                        } else {
                            Offset.Zero
                        }
                        onPositionChange(newPosition)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotationDelta ->
                    scale = (scale * zoom).coerceIn(minScale, maxScale)
                    rotation += rotationDelta
                    onScaleChange(scale)
                    onRotationChange(rotation)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun TransformableWatermark(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onSelected: () -> Unit = {},
    onPositionChange: (Offset) -> Unit = {},
    onSizeChange: (Float) -> Unit = {},
    onRotationChange: (Float) -> Unit = {},
    minSize: Float = 50f,
    maxSize: Float = 500f,
    content: @Composable () -> Unit
) {
    var size by remember { mutableFloatStateOf(100f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var center by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(with(LocalDensity.current) { size.toDp() })
            .onGloballyPositioned { coordinates ->
                val parentBounds = coordinates.parentLayoutCoordinates?.size
                if (parentBounds != null) {
                    center = Offset(
                        x = coordinates.positionInParent().x + coordinates.size.width / 2f,
                        y = coordinates.positionInParent().y + coordinates.size.height / 2f
                    )
                }
            }
            .graphicsLayer {
                scaleX = 1f
                scaleY = 1f
                rotationZ = rotation
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSelected() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onSelected() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                        onPositionChange(Offset(
                            x = (center.x + offset.x) / 1000f,
                            y = (center.y + offset.y) / 1000f
                        ))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()

        if (isSelected) {
            SelectionHandles(
                onSizeChange = { delta ->
                    size = (size + delta).coerceIn(minSize, maxSize)
                    onSizeChange(size)
                },
                onRotationChange = { delta ->
                    rotation += delta
                    onRotationChange(rotation)
                }
            )
        }
    }
}

@Composable
private fun SelectionHandles(
    onSizeChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit
) {
    Box(modifier = Modifier.matchParentSize()) {
        SelectionHandle(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(12.dp),
            onDrag = { onSizeChange(-it.x - it.y) }
        )
        SelectionHandle(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(12.dp),
            onDrag = { onSizeChange(it.x - it.y) }
        )
        SelectionHandle(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(12.dp),
            onDrag = { onSizeChange(-it.x + it.y) }
        )
        SelectionHandle(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(12.dp),
            onDrag = { onSizeChange(it.x + it.y) }
        )

        RotationHandle(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp),
            onDrag = { delta ->
                onRotationChange(delta.x * 0.5f)
            }
        )
    }
}

@Composable
private fun SelectionHandle(
    modifier: Modifier = Modifier,
    onDrag: (Offset) -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            }
    ) {}
}

@Composable
private fun RotationHandle(
    modifier: Modifier = Modifier,
    onDrag: (Offset) -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            }
    ) {}
}

@Composable
fun SnapToGuideline(
    targetOffset: Offset,
    guidelines: List<Float>,
    snapThreshold: Float = 10f,
    onSnapComplete: (Offset) -> Unit
): Offset {
    var snappedOffset = targetOffset

    guidelines.forEach { guideline ->
        if (abs(targetOffset.x - guideline) < snapThreshold) {
            snappedOffset = Offset(guideline, snappedOffset.y)
        }
        if (abs(targetOffset.y - guideline) < snapThreshold) {
            snappedOffset = Offset(snappedOffset.x, guideline)
        }
    }

    LaunchedEffect(snappedOffset) {
        onSnapComplete(snappedOffset)
    }

    return snappedOffset
}

data class AlignmentGuideline(
    val position: Float,
    val type: GuidelineType
)

enum class GuidelineType {
    VERTICAL,
    HORIZONTAL,
    CENTER
}

@Composable
fun rememberAlignmentGuideline(
    containerSize: IntSize,
    position: AlignmentPosition = AlignmentPosition.CENTER
): AlignmentGuideline {
    val guideline = when (position) {
        AlignmentPosition.LEFT -> GuidelineGuideline(containerSize.width * 0f, GuidelineType.VERTICAL)
        AlignmentPosition.CENTER_HORIZONTAL -> GuidelineGuideline(containerSize.width * 0.5f, GuidelineType.VERTICAL)
        AlignmentPosition.RIGHT -> GuidelineGuideline(containerSize.width * 1f, GuidelineType.VERTICAL)
        AlignmentPosition.TOP -> GuidelineGuideline(containerSize.height * 0f, GuidelineType.HORIZONTAL)
        AlignmentPosition.CENTER_VERTICAL -> GuidelineGuideline(containerSize.height * 0.5f, GuidelineType.HORIZONTAL)
        AlignmentPosition.BOTTOM -> GuidelineGuideline(containerSize.height * 1f, GuidelineType.HORIZONTAL)
        AlignmentPosition.CENTER -> GuidelineGuideline(containerSize.width * 0.5f, GuidelineType.CENTER)
    }
    return guideline
}

enum class AlignmentPosition {
    LEFT,
    CENTER_HORIZONTAL,
    RIGHT,
    TOP,
    CENTER_VERTICAL,
    BOTTOM,
    CENTER
}

private data class GuidelineGuideline(
    val position: Float,
    val type: GuidelineType
)
