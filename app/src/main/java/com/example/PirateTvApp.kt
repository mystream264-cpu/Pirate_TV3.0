package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.api.ApiClient

class PirateTvApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttpClient)
            .crossfade(true)
            .build()
    }
}
