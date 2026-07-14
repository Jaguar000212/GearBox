package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.security.MessageDigest

private enum class HashAlgorithm(val label: String, val jcaName: String) {
    MD5("MD5", "MD5"),
    SHA1("SHA-1", "SHA-1"),
    SHA256("SHA-256", "SHA-256"),
    SHA512("SHA-512", "SHA-512"),
}

@Composable
fun HashGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var algorithm by rememberSaveable { mutableStateOf(HashAlgorithm.SHA256.name) }
    val selectedAlgorithm = HashAlgorithm.valueOf(algorithm)

    val hash = if (input.isEmpty()) null else hashHex(input, selectedAlgorithm.jcaName)

    ToolScaffold(
        title = "Hash Generator",
        icon = Tools.byRoute(Tools.ROUTE_HASH_GENERATOR)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(HashAlgorithm.entries.size) { index ->
                val entry = HashAlgorithm.entries[index]
                FilterChip(
                    selected = selectedAlgorithm == entry,
                    onClick = { algorithm = entry.name },
                    label = { Text(entry.label) },
                )
            }
        }

        if (hash != null) {
            Spacer(Modifier.height(20.dp))
            ResultCard(
                text = hash,
                onCopy = { context.copyToClipboard(selectedAlgorithm.label, hash) },
                onShare = { context.shareText(hash) },
            )
        }
    }
}

private fun hashHex(input: String, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm).digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
