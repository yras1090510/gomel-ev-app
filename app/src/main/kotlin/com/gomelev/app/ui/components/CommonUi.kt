package com.gomelev.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import coil.compose.AsyncImage
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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
import com.gomelev.app.ui.theme.ExpressiveMotion
import com.gomelev.app.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

/** List and detail layout for compact and expanded Android windows. */
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
    val state = rememberSaveableStateHolder()
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds().onGloballyPositioned {
        origin = it.positionInWindow()
    }) {
        val feature = foldingFeature?.takeIf { it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL }
        val horizontal = feature?.orientation == FoldingFeature.Orientation.HORIZONTAL
        val vertical = feature?.orientation == FoldingFeature.Orientation.VERTICAL
        val bounds = feature?.bounds
        val inset = if (isExpandedWidth) 12.dp else 0.dp
        val gap = 12.dp
        val splitX = if (vertical && bounds != null) with(density) { (bounds.left - origin.x).toDp() } else maxWidth * 0.47f
        val rightX = if (vertical && bounds != null) with(density) { (bounds.right - origin.x).toDp() } else splitX
        val splitY = if (horizontal && bounds != null) with(density) { (bounds.top - origin.y).toDp() } else maxHeight
        val bottomY = if (horizontal && bounds != null) with(density) { (bounds.bottom - origin.y).toDp() } else splitY
        val tabletop = isExpandedWidth && horizontal && splitY > 64.dp && bottomY < maxHeight - 64.dp
        val sideBySide = isExpandedWidth && !tabletop
        val listWidth = if (sideBySide) (splitX - inset - gap / 2).coerceAtLeast(1.dp) else maxWidth - inset * 2
        val detailLeft = if (sideBySide) rightX + gap / 2 else inset
        val detailWidth = (maxWidth - detailLeft - inset).coerceAtLeast(1.dp)
        val listX by animateDpAsState(if (isExpandedWidth || !hasDetailSelection) inset else -maxWidth,
            ExpressiveMotion.defaultSpatial(), label = "catalogPosition")
        val detailX by animateDpAsState(if (isExpandedWidth || hasDetailSelection) detailLeft else maxWidth,
            ExpressiveMotion.defaultSpatial(), label = "detailPosition")
        // Keep a pane composed until it has actually cleared the window on exit.
        if (isExpandedWidth || !hasDetailSelection || listX > -maxWidth) {
            Box(Modifier.offset(x = listX).width(listWidth)
                .height(if (tabletop) splitY - gap / 2 else maxHeight).clipToBounds()) {
                state.SaveableStateProvider("list") { listPane() }
            }
        }
        if (isExpandedWidth || hasDetailSelection || detailX < maxWidth) {
            val top = if (tabletop) bottomY + gap / 2 else 0.dp
            Surface(modifier = Modifier.offset(x = detailX, y = top).width(detailWidth)
                .height((maxHeight - top).coerceAtLeast(1.dp)),
                shape = if (isExpandedWidth) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surface) {
                state.SaveableStateProvider("details") { detailPane() }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(sem.stageTop, sem.stageBottom)
                    )
                )
        ) {
            val image = car.imageRes
            val source = car.imageSource
            if (car.remotePhoto != null) {
                AsyncImage(model = car.remotePhoto, contentDescription = "Фото дилера · ${car.brandModel}",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                    error = image?.let { painterResource(it) })
            } else if (image != null) {
                val context = LocalContext.current
                Image(
                    painter = painterResource(image),
                    contentDescription = "Фото модели ${car.brandModel}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().then(
                        if (source != null && !compact) Modifier.clickable(
                            onClickLabel = "Открыть источник фотографии"
                        ) { openExternalUrl(context, source) } else Modifier
                    )
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        car.imageCredit ?: "Фото модели",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text(
                    "Фото зависит от объявления",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

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

    val animatedBg by animateColorAsState(bg, animationSpec = ExpressiveMotion.colorTween, label = "pdnBg")
    val animatedFg by animateColorAsState(fg, animationSpec = ExpressiveMotion.colorTween, label = "pdnFg")

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
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    val usedRatio = (((profile.existingHousingCreditByn + profile.otherMonthlyDebtByn) / profile.totalPdnMaxByn.coerceAtLeast(1.0))
        .toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pdnBar"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            sem.brandGradientStart.copy(alpha = if (sem.isDark) 0.12f else 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Расчётный лимит: ${Formatters.byn(profile.freePdnRoomByn)}/мес",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = sem.passAccent
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ControlMetaTag("ЗП ${Formatters.bynShort(profile.salaryByn)}")
                        ControlMetaTag("Жильё −${Formatters.bynShort(profile.existingHousingCreditByn)}")
                        ControlMetaTag("1$ = ${Formatters.rateFx(snapshot.usdBynRate)}")
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

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
                                text = "Профиль",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = sem.passContainer
            )

            // Quick PV pills always visible & wrapping cleanly on narrow screens
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.CenterVertically).padding(end = 2.dp)) {
                    Text(
                        text = "Аванс:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(ExpressiveMotion.defaultSpatial()) + fadeIn(),
                exit = shrinkVertically(ExpressiveMotion.fastSpatial()) + fadeOut()
            ) {
                Column {
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
                    text = "Платёж по кредиту за жильё: ${Formatters.byn(profile.existingHousingCreditByn)}/мес",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                )
                Slider(
                    value = profile.existingHousingCreditByn.toFloat(),
                    onValueChange = { v ->
                        onUpdateProfile { it.copy(existingHousingCreditByn = (v / 20f).roundToInt() * 20.0) }
                    },
                    valueRange = 0f..900f
                )

                Text(
                    "Другие долговые платежи: ${Formatters.byn(profile.otherMonthlyDebtByn)}/мес",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = profile.otherMonthlyDebtByn.toFloat(),
                    onValueChange = { value ->
                        onUpdateProfile { it.copy(otherMonthlyDebtByn = (value / 10f).roundToInt() * 10.0) }
                    },
                    valueRange = 0f..2000f
                )
                val context = LocalContext.current
                Text(
                    "Ориентир для авто: 40% подтверждённого дохода за вычетом долговых платежей. Банк также проверяет остаток дохода и кредитную историю.",
                    style = MaterialTheme.typography.bodySmall
                )
                androidx.compose.material3.TextButton(onClick = {
                    openExternalUrl(context, "https://etalonline.by/document/?regnum=B22035303")
                }) { Text("Правило Нацбанка") }


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
}

@Composable
private fun ControlMetaTag(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
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
