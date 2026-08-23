package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

val RemarkChartColors = listOf(
    Color(0xFF10E57A), // Electric green
    Color(0xFF4F8FFF), // Royal blue
    Color(0xFF00D2B4), // Teal
    Color(0xFFFFB300), // Amber
    Color(0xFFFF4D4D), // Red
    Color(0xFFB388FF), // Purple
    Color(0xFF80D8FF), // Light blue
    Color(0xFFFFD54F), // Yellow
    Color(0xFFFF8A80), // Coral
    Color(0xFF90A4AE), // Blue gray
    Color(0xFFA7FFEB)  // Mint
)

@Composable
fun CompanyPerformanceChart(
    companyData: List<Triple<String, Int, Int>>, // Name, Done, Remaining
    modifier: Modifier = Modifier
) {
    if (companyData.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No company data available", color = BrandTextMuted, fontSize = 12.sp)
        }
        return
    }

    val maxVal = companyData.maxOfOrNull { it.second + it.third }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        companyData.forEach { (name, done, remaining) ->
            val total = done + remaining
            val doneRatio = if (total > 0) done.toFloat() / maxVal else 0f
            val remRatio = if (total > 0) remaining.toFloat() / maxVal else 0f

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Done: $done · Rem: $remaining",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BrandTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF152445))
                ) {
                    val w = size.width
                    val h = size.height
                    val doneW = w * (done.toFloat() / total.coerceAtLeast(1))

                    // Draw Done bar (Green)
                    drawRect(
                        color = BrandGreenPrimary,
                        topLeft = Offset(0f, 0f),
                        size = Size(doneW, h)
                    )
                    // Draw Remaining bar (Navy Blue tint)
                    drawRect(
                        color = Color(0x664F8FFF),
                        topLeft = Offset(doneW, 0f),
                        size = Size(w - doneW, h)
                    )
                }
            }
        }
    }
}

@Composable
fun RemarkDistributionDonut(
    breakdown: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val items = breakdown.filter { it.value > 0 }.toList()
    val total = items.sumOf { it.second }

    if (total == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No call remarks yet", color = BrandTextMuted, fontSize = 12.sp)
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = -90f
                items.forEachIndexed { index, (_, count) ->
                    val sweep = (count.toFloat() / total) * 360f
                    val color = RemarkChartColors[index % RemarkChartColors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 18f, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "CALLS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = BrandTextMuted
                    )
                )
            }
        }

        // Legend list
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items.take(5).forEachIndexed { index, (label, count) ->
                val pct = (count.toFloat() / total) * 100f
                val color = RemarkChartColors[index % RemarkChartColors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = BrandTextSecondary
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$count (${String.format("%.0f", pct)}%)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
            if (items.size > 5) {
                Text(
                    text = "+${items.size - 5} more remarks",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = BrandTextMuted
                    ),
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}

@Composable
fun HorizontalRemarkBars(
    breakdown: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val items = breakdown.filter { it.value > 0 }.toList().sortedByDescending { it.second }
    val maxCount = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, (remark, count) ->
            val ratio = count.toFloat() / maxCount
            val color = RemarkChartColors[index % RemarkChartColors.size]

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = remark,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BrandTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF142240))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}
