package com.zhengyang.redbook.utils

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.zhengyang.redbook.R

private const val TAG = "CoilRequestExt"

fun ImageRequest.Builder.applyCircleAvatarDefaults(): ImageRequest.Builder {
    AppLogger.d(
        TAG,
        "applyCircleAvatarDefaults: enable crossfade + CircleCropTransformation"
    )
    return crossfade(true)
        .transformations(CircleCropTransformation())
}

fun ImageRequest.Builder.applyFeedCoverDefaults(
    @DrawableRes placeholderRes: Int = R.drawable.bg_xhs_image_placeholder,
    @DrawableRes errorRes: Int = R.drawable.bg_xhs_image_error,
    @DrawableRes fallbackRes: Int = R.drawable.bg_xhs_image_placeholder
): ImageRequest.Builder {
    AppLogger.d(
        TAG,
        "applyFeedCoverDefaults: placeholderRes=$placeholderRes, errorRes=$errorRes, fallbackRes=$fallbackRes, scale=${Scale.FILL}, precision=${Precision.INEXACT}"
    )
    return scale(Scale.FILL)
        .precision(Precision.INEXACT)
        .placeholder(placeholderRes)
        .error(errorRes)
        .fallback(fallbackRes)
        .crossfade(true)
}

fun ImageRequest.Builder.applyColorPlaceholderDefaults(
    @ColorRes placeholderColorRes: Int,
    @ColorRes errorColorRes: Int = placeholderColorRes,
    @ColorRes fallbackColorRes: Int = placeholderColorRes
): ImageRequest.Builder {
    AppLogger.d(
        TAG,
        "applyColorPlaceholderDefaults: placeholderColorRes=$placeholderColorRes, errorColorRes=$errorColorRes, fallbackColorRes=$fallbackColorRes"
    )
    return placeholder(placeholderColorRes)
        .error(errorColorRes)
        .fallback(fallbackColorRes)
}

fun ImageRequest.Builder.applyPreloadDefaults(): ImageRequest.Builder {
    AppLogger.d(
        TAG,
        "applyPreloadDefaults: enable memory/disk/network cache policies"
    )
    return memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
}
