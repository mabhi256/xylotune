package com.xylotune.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AccentDanger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Replaces the web's window.prompt/confirm/alert with in-theme dialogs, resolved as suspend
// calls instead of Promises — same "one modal queued at a time" shape as index.html's
// ActionDialog + askPrompt/askConfirm/showAlert.
internal sealed interface PendingModal {
    val message: String

    data class Prompt(override val message: String, val defaultValue: String, val resolve: (String?) -> Unit) : PendingModal
    data class Confirm(override val message: String, val confirmLabel: String, val danger: Boolean, val resolve: (Boolean) -> Unit) : PendingModal
    data class Alert(override val message: String, val resolve: () -> Unit) : PendingModal
}

class DialogHost {
    private var pending: PendingModal? by mutableStateOf(null)
    internal val pendingModal: PendingModal? get() = pending

    suspend fun askPrompt(message: String, defaultValue: String = ""): String? =
        suspendCancellableCoroutine { cont ->
            pending = PendingModal.Prompt(message, defaultValue) { result ->
                pending = null
                cont.resume(result)
            }
        }

    suspend fun askConfirm(message: String, confirmLabel: String = "OK", danger: Boolean = false): Boolean =
        suspendCancellableCoroutine { cont ->
            pending = PendingModal.Confirm(message, confirmLabel, danger) { result ->
                pending = null
                cont.resume(result)
            }
        }

    suspend fun showAlert(message: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            pending = PendingModal.Alert(message) {
                pending = null
                cont.resume(Unit)
            }
        }
    }
}

@Composable
fun ActionDialogHost(host: DialogHost) {
    when (val modal = host.pendingModal) {
        is PendingModal.Prompt -> {
            var value by remember(modal) { mutableStateOf(modal.defaultValue) }
            AlertDialog(
                onDismissRequest = { modal.resolve(null) },
                title = null,
                text = {
                    Column {
                        Text(modal.message, style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            singleLine = true,
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { modal.resolve(value) }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { modal.resolve(null) }) { Text("Cancel") } },
            )
        }

        is PendingModal.Confirm -> {
            AlertDialog(
                onDismissRequest = { modal.resolve(false) },
                title = null,
                text = { Text(modal.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { modal.resolve(true) }) {
                        Text(modal.confirmLabel, color = if (modal.danger) AccentDanger else MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = { TextButton(onClick = { modal.resolve(false) }) { Text("Cancel") } },
            )
        }

        is PendingModal.Alert -> {
            AlertDialog(
                onDismissRequest = { modal.resolve() },
                title = null,
                text = { Text(modal.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = { TextButton(onClick = { modal.resolve() }) { Text("OK") } },
            )
        }

        null -> Unit
    }
}
