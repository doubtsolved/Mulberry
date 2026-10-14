package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.IconStyle
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.EphemeralMulberryViewModel

@Composable
fun SettingsScreen(
    viewModel: EphemeralMulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    val currentTheme by viewModel.themeMode.collectAsState()
    val currentIconStyle by viewModel.iconStyle.collectAsState()
    val isSyncing by viewModel.isSyncingCatalog.collectAsState()

    val vaultBytes = viewModel.vaultRepository.getTotalVaultBytes()
    val snipsBytes = viewModel.snipRepository.getTotalSnipsBytes()
    val ephemeralBytes = viewModel.ephemeralCacheManager.getSessionCacheBytes()

    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1000) String.format("%.2f GB", mb / 1024.0) else String.format("%.1f MB", mb)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Settings Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Appearance • Storage Manager • CDN Status",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section 1: Appearance & Themes (All 6 Themes preserved)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FluentIcons.Settings24Regular,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Appearance & Themes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Theme Preview Grid (2 rows x 3 columns)
                        val themeList = listOf(
                            ThemeMode.OLDED to "Olded Sepia",
                            ThemeMode.PAPER to "Paper White",
                            ThemeMode.MIDNIGHT to "Pitch OLED",
                            ThemeMode.FOREST to "Forest Sage",
                            ThemeMode.ESPRESSO to "Espresso Mocha",
                            ThemeMode.DUSK to "Dusk Lavender"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                themeList.take(3).forEach { (mode, label) ->
                                    ThemeTile(
                                        title = label,
                                        isSelected = currentTheme == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                themeList.drop(3).forEach { (mode, label) ->
                                    ThemeTile(
                                        title = label,
                                        isSelected = currentTheme == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Icon Style Toggle (Vibrant vs Adaptive)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Icon Style",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentIconStyle == IconStyle.VIBRANT) colors.primary else colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                    modifier = Modifier.clickable { viewModel.setIconStyle(IconStyle.VIBRANT) }
                                ) {
                                    Text(
                                        text = "Vibrant",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentIconStyle == IconStyle.VIBRANT) colors.onPrimary else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentIconStyle == IconStyle.ADAPTIVE) colors.primary else colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                    modifier = Modifier.clickable { viewModel.setIconStyle(IconStyle.ADAPTIVE) }
                                ) {
                                    Text(
                                        text = "Adaptive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentIconStyle == IconStyle.ADAPTIVE) colors.onPrimary else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Storage Footprint Manager
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FluentIcons.ArrowDownload24Regular,
                                contentDescription = null,
                                tint = colors.accentWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Storage Footprint Manager",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        StorageMetricRow(label = "Encrypted Vault (.bin)", value = formatBytes(vaultBytes))
                        StorageMetricRow(label = "Diagram Snips (.webp)", value = formatBytes(snipsBytes))
                        StorageMetricRow(label = "Ephemeral Plaintext Cache", value = formatBytes(ephemeralBytes))

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.ephemeralCacheManager.purgeEphemeralSession()
                                Toast.makeText(context, "Ephemeral session cache wiped", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Purge Ephemeral Sessions", fontSize = 13.sp, color = colors.textPrimary)
                        }
                    }
                }
            }

            // Section 3: CDN & Sync Status
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FluentIcons.ArrowSync24Regular,
                                contentDescription = null,
                                tint = colors.accentSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edge CDN & Sync Topology",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CdnStatusRow(name = "jsDelivr Edge (Global CDN)", status = "Online • Fastly/Cloudflare")
                        CdnStatusRow(name = "GitHub Raw Origin (Fallback)", status = "Standby • Auto Fallback")

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.syncCatalog { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = colors.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...", color = colors.onPrimary)
                            } else {
                                Text("Sync Catalog Now", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 4: Community & Submission
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Community & Ingestion",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Submit new textbooks or lecture notes to the catalog via our automated GitHub Actions pipeline.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MulberryIngestionBot"))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Telegram Ingestion Bot", fontSize = 13.sp, color = colors.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ThemeTile(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.primary else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.borderSubtle
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.onPrimary else colors.textPrimary
            )
        }
    }
}

@Composable
fun StorageMetricRow(label: String, value: String) {
    val colors = LocalMulberryColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = colors.textSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

@Composable
fun CdnStatusRow(name: String, status: String) {
    val colors = LocalMulberryColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 12.sp, color = colors.textPrimary)
        Text(text = status, fontSize = 11.sp, color = colors.accentSuccess)
    }
}
