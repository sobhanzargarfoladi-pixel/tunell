package com.foxyvpn.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxyvpn.app.FoxyVpnApp
import com.foxyvpn.app.data.GUARDIAN_ENDPOINT_DEFAULT
import com.foxyvpn.app.data.GuardianClient
import com.foxyvpn.app.data.formatBytes
import com.foxyvpn.app.data.model.ConnectionState
import com.foxyvpn.app.ui.components.FoxyGradient
import com.foxyvpn.app.ui.components.FoxyWordmark
import com.foxyvpn.app.ui.theme.FoxSeed
import com.foxyvpn.app.ui.theme.LocalFoxyStatusColors
import com.foxyvpn.app.ui.theme.ThemeController
import com.foxyvpn.app.ui.theme.ThemeMode
import com.foxyvpn.app.vpn.FoxyVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface QuotaUi {
    data object Loading : QuotaUi
    data object Error : QuotaUi
    data object Unlimited : QuotaUi
    data class Data(val remaining: Long?, val max: Long?) : QuotaUi
}

private suspend fun loadQuota(app: FoxyVpnApp): QuotaUi = withContext(Dispatchers.IO) {
    runCatching<QuotaUi> {
        val token = app.authRepository.currentAccessToken() ?: return@runCatching QuotaUi.Error
        val e = GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, token)
        if (!e.limitedBandwidth) QuotaUi.Unlimited else QuotaUi.Data(e.quotaRemaining, e.maxBytes)
    }.getOrElse { QuotaUi.Error }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by FoxyVpnService.state.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()
    var quota by remember { mutableStateOf<QuotaUi>(QuotaUi.Loading) }

    LaunchedEffect(state) {
        quota = loadQuota(app)
        while (state == ConnectionState.CONNECTED) {
            delay(60_000)
            quota = loadQuota(app)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme
    val statusColors = LocalFoxyStatusColors.current

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070710), Color(0xFF0B0A18), Color(0xFF05050B))))) {
        LiquidBackground(active = state != ConnectionState.DISCONNECTED)
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { FoxyWordmark(size = 27.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        val dark = themeController.resolveDark(systemDark)
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.075f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                                .combinedClickable(
                                    role = Role.Button,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        themeController.toggle(systemDark)
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        themeController.set(ThemeMode.SYSTEM)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if (dark) Icons.Filled.DarkMode else Icons.Filled.LightMode, null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(5.dp))
                        IconButton(onClick = onOpenSettings) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.075f)).border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Settings, "Settings", tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusPill(state)
                Spacer(Modifier.height(11.dp))
                QuotaGlassCard(quota)
                Spacer(Modifier.height(10.dp))
                PowerButton(state) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                }
                Text(
                    when (state) {
                        ConnectionState.CONNECTED -> "Connected"
                        ConnectionState.CONNECTING -> "Connecting…"
                        ConnectionState.DISCONNECTED -> "Not connected"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when (state) {
                        ConnectionState.CONNECTED -> "Tunnela is protecting your connection"
                        ConnectionState.CONNECTING -> "Creating a secure tunnel…"
                        ConnectionState.DISCONNECTED -> "Tap the button to connect securely"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center,
                )
                lastError?.let { error ->
                    if (state != ConnectionState.CONNECTING) {
                        Spacer(Modifier.height(7.dp))
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, null, tint = Color(0xFFFF9C9C), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(error, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                LocationGlassCard(
                    name = selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
                    connected = state == ConnectionState.CONNECTED,
                    statusColor = statusColors.connected,
                    onClick = onOpenServers,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniGlassCard(Modifier.weight(1f), Icons.Filled.Speed, "Smart route", "Optimized")
                    MiniGlassCard(Modifier.weight(1f), Icons.Filled.Security, "Protection", if (state == ConnectionState.CONNECTED) "Active" else "Ready")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LiquidBackground(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "liquid-bg")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    Canvas(Modifier.fillMaxSize().blur(38.dp)) {
        val w = size.width
        val h = size.height
        val a = if (active) 0.25f else 0.14f
        drawCircle(Color(0xFF7B4DFF).copy(alpha = a), radius = 190.dp.toPx(), center = Offset(w * (0.18f + shift * 0.10f), h * 0.14f))
        drawCircle(Color(0xFF39C6FF).copy(alpha = a * 0.70f), radius = 150.dp.toPx(), center = Offset(w * (0.90f - shift * 0.12f), h * 0.40f))
        drawCircle(Color(0xFFFF5E9A).copy(alpha = a * 0.55f), radius = 170.dp.toPx(), center = Offset(w * 0.72f, h * (0.88f - shift * 0.08f)))
        drawCircle(Color(0xFFFFA24C).copy(alpha = a * 0.35f), radius = 125.dp.toPx(), center = Offset(w * 0.08f, h * 0.75f))
    }
}

@Composable
private fun StatusPill(state: ConnectionState) {
    val accent by animateColorAsState(
        when (state) {
            ConnectionState.CONNECTED -> Color(0xFF6EF7B0)
            ConnectionState.CONNECTING -> Color(0xFF6EC8FF)
            ConnectionState.DISCONNECTED -> Color(0xFFFFB15E)
        }, label = "status-accent"
    )
    GlassCard(
        Modifier,
        fill = Color.White.copy(alpha = 0.055f),
        border = accent.copy(alpha = 0.25f),
        radius = 50.dp,
        paddingH = 14.dp,
        paddingV = 8.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent).shadow(8.dp, CircleShape, ambientColor = accent, spotColor = accent))
            Spacer(Modifier.width(8.dp))
            Text(
                when (state) {
                    ConnectionState.CONNECTED -> "SECURE CONNECTION"
                    ConnectionState.CONNECTING -> "CONNECTING"
                    ConnectionState.DISCONNECTED -> "READY TO CONNECT"
                },
                color = Color.White.copy(alpha = 0.86f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.15.sp,
            )
        }
    }
}

@Composable
private fun QuotaGlassCard(quota: QuotaUi) {
    val fraction = (quota as? QuotaUi.Data)?.let { d -> if (d.remaining != null && d.max != null && d.max > 0) (d.remaining.toFloat() / d.max).coerceIn(0f, 1f) else null }
    val animated by animateFloatAsState(fraction ?: 0f, animationSpec = tween(700, easing = FastOutSlowInEasing), label = "quota")
    GlassCard(Modifier.fillMaxWidth(), paddingH = 16.dp, paddingV = 13.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(FoxyGradient), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.DataUsage, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("DATA REMAINING", color = Color.White.copy(alpha = 0.48f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        when (quota) {
                            QuotaUi.Loading -> "…"
                            QuotaUi.Error -> "—"
                            QuotaUi.Unlimited -> "Unlimited"
                            is QuotaUi.Data -> when {
                                quota.remaining != null -> formatBytes(quota.remaining)
                                quota.max != null -> "Up to ${formatBytes(quota.max)}"
                                else -> "Limited"
                            }
                        },
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                    )
                    if (quota is QuotaUi.Data && quota.remaining != null && quota.max != null) {
                        Spacer(Modifier.width(6.dp))
                        Text("/ ${formatBytes(quota.max)}", color = Color.White.copy(alpha = 0.42f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
            }
            if (fraction != null) {
                Box(Modifier.size(43.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("${(fraction * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (fraction != null) {
            Spacer(Modifier.height(11.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f))) {
                Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(CircleShape).background(FoxyGradient))
            }
        }
    }
}

@Composable
private fun PowerButton(state: ConnectionState, onClick: () -> Unit) {
    val accent by animateColorAsState(
        when (state) {
            ConnectionState.CONNECTED -> Color(0xFF5EF2A7)
            ConnectionState.CONNECTING -> Color(0xFF66C8FF)
            ConnectionState.DISCONNECTED -> Color(0xFFFFA24F)
        }, label = "power"
    )
    val active = state != ConnectionState.DISCONNECTED
    val transition = rememberInfiniteTransition(label = "power-pulse")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if (state == ConnectionState.CONNECTING) 1200 else 2600, easing = LinearEasing), RepeatMode.Restart), label = "pulse")

    Box(Modifier.size(226.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val base = 73.dp.toPx()
            val max = 103.dp.toPx()
            drawCircle(accent.copy(alpha = 0.06f), radius = 112.dp.toPx(), center = c)
            drawCircle(accent.copy(alpha = 0.18f), radius = base + 15.dp.toPx(), center = c, style = Stroke(width = 1.5.dp.toPx()))
            if (active) {
                repeat(2) { i ->
                    val t = (pulse + i * 0.5f) % 1f
                    drawCircle(accent.copy(alpha = (1f - t) * 0.28f), radius = base + (max - base) * t, center = c, style = Stroke(width = 2.3.dp.toPx()))
                }
            }
        }
        Box(
            Modifier
                .size(148.dp)
                .shadow(if (active) 28.dp else 20.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(lerp(accent, Color.White, 0.26f), accent, lerp(accent, Color.Black, 0.24f))))
                .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(128.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.12f)).border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PowerSettingsNew, if (state == ConnectionState.DISCONNECTED) "Connect" else "Disconnect", tint = Color.White, modifier = Modifier.size(62.dp))
            }
        }
    }
}

@Composable
private fun LocationGlassCard(name: String, connected: Boolean, statusColor: Color, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick), paddingH = 14.dp, paddingV = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).border(1.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Public, null, tint = Color(0xFFBFAEFF), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LOCATION", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    if (connected) {
                        Spacer(Modifier.width(7.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun MiniGlassCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    GlassCard(modifier, paddingH = 12.dp, paddingV = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White.copy(alpha = 0.43f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier,
    fill: Color = Color.White.copy(alpha = 0.065f),
    border: Color = Color.White.copy(alpha = 0.105f),
    radius: androidx.compose.ui.unit.Dp = 22.dp,
    paddingH: androidx.compose.ui.unit.Dp = 14.dp,
    paddingV: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(fill)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(radius))
            .padding(horizontal = paddingH, vertical = paddingV),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent))))
            Spacer(Modifier.height(5.dp))
            content()
        }
    }
}
