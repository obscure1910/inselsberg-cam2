package de.obscure.webcam.overview.photo

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule

@GlideModule
open class MyGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        val cacheSize20MegaBytes = 20971520
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(context, cacheSize20MegaBytes.toLong())
        )
    }

}
