package com.kalidroid.ui.init

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kali 初始化动画页。
 * 核心资源（proot 引擎）下载完成前不允许进入主界面。
 * 视觉：毒液紫黑渐变 + 旋转光环 + 脉冲核心 + 进度环动画。
 */
@Composable
fun InitScreen(
    downloading: Boolean,
    progress: Float,
    status: String,
    sourceName: String = "默认",
    onSwitchSource: () -> Unit = {},
    onInit: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "init")
    // 光环旋转
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "ring"
    )
    // 核心脉冲
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // 呼吸闪烁
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0118), Color(0xFF1A0B2E), Color(0xFF2E0A4E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ---- 中心动画 Logo：旋转光环 + 脉冲核心 ----
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(170.dp)) {
                    // 外光环（旋转点）
                    val ringR = size.minDimension / 2 - 8.dp.toPx()
                    val rad = Math.toRadians(angle.toDouble())
                    val dotX = center.x + ringR * cos(rad).toFloat()
                    val dotY = center.y + ringR * sin(rad).toFloat()
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = glow),
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                    // 轨道
                    drawCircle(
                        color = Color(0xFF7C4DFF).copy(alpha = 0.35f),
                        radius = ringR,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // 内轨道
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.25f),
                        radius = ringR * 0.72f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                // 核心脉冲块
                Box(
                    modifier = Modifier
                        .size((64 * pulse).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF00E676).copy(alpha = 0.85f), Color(0xFF7C4DFF)),
                                center = Offset(0.5f, 0.5f),
                                radius = 0.9f
                            ),
                            shape = MaterialTheme.shapes.large
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "K",
                        color = Color.White,
                        fontSize = (30 * pulse).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ---- 标题 ----
            Text(
                "KALI DROID",
                color = Color(0xFF00E676),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "渗透测试移动工作台",
                color = Color(0xFFB39DDB),
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(36.dp))

            // ---- 进度区 ----
            if (downloading) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(56.dp),
                    color = Color(0xFF00E676),
                    trackColor = Color(0xFF7C4DFF).copy(alpha = 0.25f),
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color(0xFF00E676),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                // 未开始：脉冲圆点 + 按钮
                Box(Modifier.size(10.dp).background(Color(0xFF00E676).copy(alpha = glow), MaterialTheme.shapes.small))
            }

            Spacer(Modifier.height(10.dp))
            Text(status, color = Color(0xFFB39DDB), fontSize = 13.sp)

            Spacer(Modifier.height(28.dp))

            // ---- 一键初始化按钮 ----
            if (!downloading) {
                Button(
                    onClick = onInit,
                    modifier = Modifier.padding(horizontal = 40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 34.dp, vertical = 14.dp)
                ) {
                    Text("⚡ 一键初始化（下载核心资源）", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                // 当前源 + 换源入口
                Spacer(Modifier.height(8.dp))
                Text(
                    "当前下载源：$sourceName",
                    color = Color(0xFF9575CD),
                    fontSize = 12.sp
                )
                OutlinedButton(
                    onClick = onSwitchSource,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Text("🔄 切换下载源", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "需先下载 proot 引擎后才能使用",
                color = Color(0xFF9575CD).copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}