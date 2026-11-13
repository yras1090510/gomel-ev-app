package com.gomelev.app.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.MarketSnapshot
import com.gomelev.app.model.PdnVerdict
import com.gomelev.app.model.UserFinanceProfile
import com.gomelev.app.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

/**
 * Galaxy Z Fold 8 / Foldable-aware split layout.
 * - Unfolded vertical hinge or large screen (>= 620dp): dual pane respecting physical crease bounds.
 * - Half-opened tabletop hinge: top studio view + bottom interactive pane.
 * - Cover screen (< 620dp): crisp single pane or overlay detail.
 */
@Composable
fun FoldableTwoPane(
    isExpandedWidth: Boolean,
    foldingFeature: FoldingFeature?,
    hasDetailSelection: Boolean,
    modifier: Modifier = Modifier,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val hingeBoundsPx: Rect? = foldingFeature?.bounds
    val isVerticalHinge = foldingFeature?.orientation == FoldingFeature.Orientation.VERTICAL
    val hingeGapDp: Dp = if (isVerticalHinge && hingeBoundsPx != null) {
        with(density) { hingeBoundsPx.width().toDp().coerceIn(12.dp, 32.dp) }
    } else {
        16.dp
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showTwoPane = isExpandedWidth && maxWidth >= 600.dp
        if (showTwoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(hingeGapDp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.47f)
                        .fillMaxHeight()
                ) {
                    listPane()
                }
                Box(
                    modifier = Modifier
                        .weight(0.53f)
                        .fillMaxHeight()
                ) {
                    detailPane()
                }
            }
        } else {
            if (hasDetailSelection) {
                detailPane()
            } else {
                listPane()
            }
        }
    }
}

@Composable
fun MinimalistCarStage(
    car: CarOffer,
    usdBynRate: Double,
    compact: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sem = LocalSemanticColors.current
    val height = if (compact) 128.dp else 188.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(18.dp),
        color = sem.studioBackdrop,
        border = BorderStroke(0.8.dp, sem.studioBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle gallery softbox spotlight radial backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Minimalist studio car vector artwork
            Image(
                painter = painterResource(id = car.imageRes),
                contentDescription = car.brandModel,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (compact) 18.dp else 28.dp,
                        vertical = if (compact) 10.dp else 16.dp
                    )
            )

            // Top-left minimal fuel badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(0.5.dp, sem.studioBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (car.isEv) Icons.Rounded.Bolt else Icons.Rounded.LocalGasStation,
                        contentDescription = null,
                        tint = if (car.isEv) sem.passAccent else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (car.isEv) "EV" else "ДВС",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Top-right USD badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(0.5.dp, sem.studioBorder)
            ) {
                Text(
                    text = Formatters.usdFromByn(car.priceByn, usdBynRate),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PdnStatusPill(
    verdict: PdnVerdict,
    monthlyByn: Double,
    modifier: Modifier = Modifier
) {
    val sem = LocalSemanticColors.current
    val (bg, fg, icon) = when (verdict) {
        PdnVerdict.PASS -> Triple(sem.passContainer, sem.onPassContainer, Icons.Rounded.CheckCircle)
        PdnVerdict.BORDERLINE -> Triple(sem.warnContainer, sem.onWarnContainer, Icons.Rounded.WarningAmber)
        PdnVerdict.FAIL -> Triple(sem.failContainer, sem.onFailContainer, Icons.Rounded.ErrorOutline)
    }

    val animatedBg by animateColorAsState(bg, label = "pdnBg")
    val animatedFg by animateColorAsState(fg, label = "pdnFg")

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = animatedBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedFg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${verdict.labelRu} · ${Formatters.bynShort(monthlyByn)}/м",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold
                ),
                color = animatedFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PdnControlBar(
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sem = LocalSemanticColors.current
    var expanded by remember { mutableStateOf(false) }

    val usedRatio = ((profile.existingHousingCreditByn / profile.totalPdnMaxByn.coerceAtLeast(1.0))
        .toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pdnBar"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Свободный лимит ПДН: ${Formatters.byn(profile.freePdnRoomByn)}/мес",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = sem.passAccent
                    )
                    Text(
                        text = "ЗП ${Formatters.bynShort(profile.salaryByn)} · Указ №240: −${Formatters.bynShort(profile.existingHousingCreditByn)} · 1$ = ${Formatters.rateFx(snapshot.usdBynRate)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTriggerSync, enabled = !snapshot.isSyncing) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Синхронизировать сайты",
                            tint = if (snapshot.isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { expanded = !expanded }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "ПВ ${profile.downPaymentPct}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = sem.passContainer
            )

            // Quick PV pills always visible
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Аванс:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(0, 10, 15, 20).forEach { pct ->
                    val selected = !profile.useCustomDownPaymentByn && profile.downPaymentPct == pct
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateProfile {
                                it.copy(downPaymentPct = pct, useCustomDownPaymentByn = false)
                            }
                        },
                        label = {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Зарплата (чистыми): ${Formatters.byn(profile.salaryByn)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                )
                Slider(
                    value = profile.salaryByn.toFloat(),
                    onValueChange = { v ->
                        onUpdateProfile { it.copy(salaryByn = (v / 100f).roundToInt() * 100.0) }
                    },
                    valueRange = 1500f..6000f
                )

                Text(
                    text = "Действующие кредиты (Указ №240): ${Formatters.byn(profile.existingHousingCreditByn)}/мес",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                )
                Slider(
                    value = profile.existingHousingCreditByn.toFloat(),
                    onValueChange = { v ->
                        onUpdateProfile { it.copy(existingHousingCreditByn = (v / 20f).roundToInt() * 20.0) }
                    },
                    valueRange = 0f..900f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Жёсткий скоринг от полной ставки",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Считать ПДН без учёта уменьшения тела в грейс-период",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.forceConservativeP0,
                        onCheckedChange = { chk ->
                            onUpdateProfile { it.copy(forceConservativeP0 = chk) }
                        }
                    )
                }
            }
        }
    }
}

fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

fun dialPhone(context: Context, phone: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }
}

fun openGeoLocation(context: Context, geoUri: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)))
    }
}
