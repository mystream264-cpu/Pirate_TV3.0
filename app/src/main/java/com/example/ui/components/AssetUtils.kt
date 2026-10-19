package com.example.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

object AssetUtils {

    fun assetExists(context: Context, filename: String): Boolean {
        return try {
            context.assets.open(filename).use { true }
        } catch (_: Exception) {
            false
        }
    }

    fun assetUri(filename: String): String {
        return "file:///android_asset/$filename"
    }

    @Composable
    fun rememberAssetExists(filename: String): Boolean {
        val context = LocalContext.current
        return remember(filename) {
            assetExists(context, filename)
        }
    }
}
