package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.RandomResultSnapshots
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.UiPreferencesStore
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FlipCoinScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val uiPreferencesStore = remember(context) { UiPreferencesStore(context) }
    val uiPreferences by uiPreferencesStore.preferences.collectAsState()
    var runId by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<Boolean?>(null) }
    // Transient animation state is not restored: recreation must never leave controls locked.
    var flipping by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val resultText = when (result) { true -> context.getString(R.string.game_heads); false -> context.getString(R.string.game_tails); null -> context.getString(R.string.game_ready_to_flip) }

    val history = rememberToolHistory(
        route = Tools.ROUTE_FLIP_COIN,
        inputs = mapOf("coin" to "Fair coin", "_run" to runId, "_heads" to (result?.toString() ?: "")),
        output = resultText.takeIf { !flipping && result != null },
        recordingDelayMillis = 0L,
        onRestore = { saved ->
            if (!flipping) {
                result = RandomResultSnapshots.coin(saved)
                runId = saved["_run"].orEmpty()
            }
        },
    )

    ToolScaffold(context.getString(R.string.game_flip_coin), Tools.byRoute(Tools.ROUTE_FLIP_COIN)!!.icon, onNavigateBack, history = history) {
        Text(context.getString(R.string.game_heads_or_tails), style = MaterialTheme.typography.titleLarge)
        Text(context.getString(R.string.game_one_coin_two_equally_likely_outcomes), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(176.dp)
                    .graphicsLayer { rotationY = rotation.value; cameraDistance = 12f * density }
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (flipping) "?" else when (result) { true -> context.getString(R.string.game_heads_symbol); false -> context.getString(R.string.game_tails_symbol); null -> "?" },
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (flipping) context.getString(R.string.game_flipping) else resultText,
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !flipping,
            onClick = {
                if (!flipping) {
                    flipping = true
                    result = null
                    val outcome = Random.nextBoolean()
                    scope.launch {
                        try {
                            rotation.snapTo(0f)
                            // Animatable honors the system animation duration scale, including zero.
                            // Whole turns avoid mirrored text and reset each flip to avoid float drift.
                            if (!uiPreferences.reduceMotion) rotation.animateTo(720f, tween(500))
                            runId = java.util.UUID.randomUUID().toString()
                            result = outcome
                        } finally {
                            flipping = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (flipping) context.getString(R.string.game_flipping) else if (result == null) context.getString(R.string.game_flip_coin_133) else context.getString(R.string.game_flip_again))
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { context.copyToClipboard(context.getString(R.string.game_coin_flip), resultText) },
            onShare = { context.shareText(context.getString(R.string.game_coin_flip_1_s, resultText)) },
            onClear = { result = null },
            resultEnabled = !flipping && result != null,
            clearEnabled = !flipping && result != null,
        )
    }
}
