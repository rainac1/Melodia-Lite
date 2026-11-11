package com.lin0721.linmusic.feature.player.ui

import androidx.compose.material.icons.rounded.MoreVert
import android.media.AudioDeviceInfo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MelodiaIconButton
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MelodiaColors
import com.lin0721.linmusic.core.ui.theme.MelodiaPress
import com.lin0721.linmusic.core.ui.theme.InfoCardRadius
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

import androidx.compose.ui.text.font.FontWeight

// 插件布局用的操作行：按给定按钮与顺序排布
@Composable
fun PlayerActionRow(
    buttons: List<String>,
    onOutputDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreClick: () -> Unit,
    connectedDevice: AudioDeviceInfo?,
    qualityLabel: String?,
    modifier: Modifier = Modifier,
    onSleepTimerClick: () -> Unit = {},
    sleepTimerActive: Boolean = false,
    onCommentsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEach { button ->
            when (button) {
                "device" -> MelodiaIconButton(onClick = onOutputDeviceClick) {
                    Icon(
                        connectedDevice?.let { deviceIcon(it.type) } ?: Icons.Rounded.SpeakerGroup,
                        contentDescription = "连接设备",
                        tint = if (connectedDevice != null) MaterialTheme.colorScheme.primary else MelodiaColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                "queue" -> MelodiaIconButton(onClick = onQueueClick) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = "播放队列", tint = MelodiaColors.TextSecondary, modifier = Modifier.size(30.dp))
                }
                "quality" -> if (qualityLabel != null) {
                    Text(qualityLabel, color = MelodiaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                "more" -> MelodiaIconButton(onClick = onMoreClick) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "更多", tint = MelodiaColors.TextSecondary, modifier = Modifier.size(24.dp))
                }
                "sleepTimer" -> MelodiaIconButton(onClick = onSleepTimerClick) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "定时停止",
                        tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else MelodiaColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                "comments" -> MelodiaIconButton(onClick = onCommentsClick) {
                    Icon(Icons.AutoMirrored.Rounded.Comment, contentDescription = "评论", tint = MelodiaColors.TextSecondary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// 控件区底部次级入口：输出设备、播放队列
// connectedDevice 非空时说明当前连着非扬声器设备（有线/蓝牙），图标换类型、变主题色，并在图标后显示设备名；扬声器播放时保持默认
@Composable
fun ActionButtons(
    onOutputDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    connectedDevice: AudioDeviceInfo? = null,
    // 实际音质短标签，为空时不显示
    qualityLabel: String? = null
) {
    val outputDeviceIcon = connectedDevice?.let { deviceIcon(it.type) } ?: Icons.Rounded.SpeakerGroup
    val outputDeviceTint = if (connectedDevice != null) MaterialTheme.colorScheme.primary else MelodiaColors.TextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.lg)
            .padding(top = MelodiaSpacing.sm, bottom = MelodiaSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    // 反馈交给里面的图标按钮，外层只是把设备名也纳入点击区
                    .pressable(MelodiaPress.None, onClick = onOutputDeviceClick)
            ) {
                MelodiaIconButton(
                    onClick = onOutputDeviceClick,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(outputDeviceIcon, contentDescription = "连接设备", tint = outputDeviceTint, modifier = Modifier.size(24.dp))
                }
                if (connectedDevice != null) {
                    Text(
                        text = deviceLabel(connectedDevice),
                        color = outputDeviceTint,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .offset(x = (-8).dp)
                            .widthIn(max = 140.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.xs)
            ) {
                MelodiaIconButton(
                    onClick = onQueueClick,
                    modifier = Modifier.offset(x = 12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = MelodiaColors.TextSecondary, modifier = Modifier.size(30.dp))
                }
            }
        }
        if (qualityLabel != null) {
            Text(
                text = qualityLabel,
                color = MelodiaColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
