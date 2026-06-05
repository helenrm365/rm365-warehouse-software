package com.example.rm365warehousesoftware.ui.failed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rm365warehousesoftware.data.local.ScanEntity
import com.example.rm365warehousesoftware.ui.theme.RM365WarehouseSoftwareTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that lists scans the server rejected, letting the warehouse worker
 * dismiss them or reset them for another sync attempt.
 */
@Composable
fun FailedScansScreen(
    viewModel: FailedScansViewModel,
    modifier: Modifier = Modifier
) {
    val failedScans by viewModel.failedScans.collectAsStateWithLifecycle()
    FailedScansContent(
        scans = failedScans,
        onDismiss = viewModel::dismiss,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@Composable
private fun FailedScansContent(
    scans: List<ScanEntity>,
    onDismiss: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Id of the scan pending deletion confirmation, or null if no dialog shown.
    var pendingDeletionId by remember { mutableStateOf<Long?>(null) }

    if (scans.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No failed scans to review",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = scans, key = { it.id }) { scan ->
            FailedScanCard(
                scan = scan,
                onDismiss = { pendingDeletionId = scan.id },
                onRetry = { onRetry(scan.id) }
            )
        }
    }

    pendingDeletionId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeletionId = null },
            title = { Text("Delete this scan?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss(id)
                        pendingDeletionId = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletionId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FailedScanCard(
    scan: ScanEntity,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = scan.barcode,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatTimestamp(scan.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.padding(top = 8.dp))
            Text(
                text = scan.syncError ?: "Unknown error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            if (scan.attempts > 0) {
                Text(
                    text = "Attempts: ${scan.attempts}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

@Preview(showBackground = true)
@Composable
private fun FailedScansContentPreview() {
    RM365WarehouseSoftwareTheme {
        FailedScansContent(
            scans = listOf(
                ScanEntity(
                    id = 1,
                    barcode = "0123456789012",
                    timestamp = 1_733_400_000_000,
                    syncError = "Unknown SKU",
                    attempts = 2
                ),
                ScanEntity(
                    id = 2,
                    barcode = "5901234123457",
                    timestamp = 1_733_400_500_000,
                    syncError = "Location closed for stocktake",
                    attempts = 1
                )
            ),
            onDismiss = {},
            onRetry = {}
        )
    }
}
