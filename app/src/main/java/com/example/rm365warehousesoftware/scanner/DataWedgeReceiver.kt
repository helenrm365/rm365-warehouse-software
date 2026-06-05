package com.example.rm365warehousesoftware.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives barcode data broadcast by Zebra DataWedge.
 *
 * Configure your DataWedge profile's Intent Output plugin with:
 *  - Intent action:   [ACTION_DATAWEDGE_RESULT]
 *  - Intent delivery: "Broadcast intent"
 *
 * The scanned value is delivered in the [EXTRA_DATA_STRING] extra.
 */
class DataWedgeReceiver(
    private val onBarcodeScanned: (barcode: String, symbology: String?) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DATAWEDGE_RESULT -> {
                val barcode = intent.getStringExtra(EXTRA_DATA_STRING)
                val symbology = intent.getStringExtra(EXTRA_LABEL_TYPE)
                if (!barcode.isNullOrBlank()) {
                    onBarcodeScanned(barcode, symbology)
                }
            }
        }
    }

    companion object {
        /** Action used both for the DataWedge API result and the scan data output. */
        const val ACTION_DATAWEDGE_RESULT = "com.symbol.datawedge.api.RESULT_ACTION"

        /** Category set on the DataWedge Intent Output profile. */
        const val CATEGORY_DATAWEDGE = "android.intent.category.DEFAULT"

        /** Extra holding the decoded barcode string. */
        const val EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"

        /** Extra holding the barcode symbology (e.g. EAN13, CODE128). */
        const val EXTRA_LABEL_TYPE = "com.symbol.datawedge.label_type"
    }
}
