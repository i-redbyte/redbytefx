package ru.redbyte.redbytefx.sample.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** API 29 — scoped storage; gallery URIs from the picker usually need no legacy read grant. */
internal const val SDK_SCOPED_STORAGE: Int = Build.VERSION_CODES.Q

/** API 33 — photo picker and [Manifest.permission.READ_MEDIA_IMAGES]. */
internal const val SDK_MEDIA_IMAGES_PERMISSION: Int = Build.VERSION_CODES.TIRAMISU

/**
 * Runtime permissions the sample may request before opening the legacy gallery picker.
 * On API 33+ the system photo picker is used and does not require these.
 */
internal fun readImagePermissionNames(sdkInt: Int): List<String> = when {
    sdkInt >= SDK_MEDIA_IMAGES_PERMISSION -> listOf(Manifest.permission.READ_MEDIA_IMAGES)
    sdkInt < SDK_SCOPED_STORAGE -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    else -> emptyList()
}

internal fun usesSystemPhotoPicker(sdkInt: Int): Boolean = sdkInt >= SDK_MEDIA_IMAGES_PERMISSION

internal fun hasReadImageAccess(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
    val required = readImagePermissionNames(sdkInt)
    if (required.isEmpty()) return true
    return required.all { name ->
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
    }
}
