package com.example.rm365warehousesoftware

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rm365warehousesoftware.data.repository.ScanResult
import com.example.rm365warehousesoftware.scanner.DataWedgeReceiver
import com.example.rm365warehousesoftware.ui.failed.FailedScansScreen
import com.example.rm365warehousesoftware.ui.failed.FailedScansViewModel
import com.example.rm365warehousesoftware.ui.theme.RM365WarehouseSoftwareTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository by lazy { (application as WarehouseApplication).repository }

    private var lastScannedBarcode by mutableStateOf<String?>(null)

    private val dataWedgeReceiver = DataWedgeReceiver { barcode, _ ->
        lastScannedBarcode = barcode
        onBarcodeScanned(barcode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RM365WarehouseSoftwareTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val failedScansViewModel: FailedScansViewModel = viewModel(
                        factory = FailedScansViewModel.Factory(repository)
                    )
                    HomeScreen(
                        barcode = lastScannedBarcode,
                        failedScansViewModel = failedScansViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(DataWedgeReceiver.ACTION_DATAWEDGE_RESULT)
            addCategory(DataWedgeReceiver.CATEGORY_DATAWEDGE)
        }
        // The receiver only handles our own DataWedge profile output, so it is
        // registered as NOT_EXPORTED to avoid exposure to other apps.
        ContextCompat.registerReceiver(
            this,
            dataWedgeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Flush anything that queued while we were offline.
        lifecycleScope.launch { repository.syncPendingScans() }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(dataWedgeReceiver)
    }

    private fun onBarcodeScanned(barcode: String) {
        lifecycleScope.launch {
            when (repository.processScan(barcode)) {
                is ScanResult.Uploaded -> { /* TODO: show success feedback */ }
                is ScanResult.Queued -> { /* TODO: indicate scan was queued offline */ }
                is ScanResult.ServerError -> { /* TODO: surface server error */ }
            }
        }
    }
}

@Composable
fun HomeScreen(
    barcode: String?,
    failedScansViewModel: FailedScansViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scan", "Failed")
    val failedCount by failedScansViewModel.failedCount.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        if (index == 1 && failedCount > 0) {
                            BadgedBox(badge = { Badge { Text("$failedCount") } }) {
                                Text(title)
                            }
                        } else {
                            Text(title)
                        }
                    }
                )
            }
        }
        when (selectedTab) {
            0 -> ScanScreen(
                barcode = barcode,
                modifier = Modifier.padding(16.dp)
            )
            1 -> FailedScansScreen(viewModel = failedScansViewModel)
        }
    }
}

@Composable
fun ScanScreen(barcode: String?, modifier: Modifier = Modifier) {
    Text(
        text = barcode?.let { "Scanned: $it" } ?: "Waiting for scan...",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    RM365WarehouseSoftwareTheme {
        ScanScreen(barcode = "0123456789012")
    }
}