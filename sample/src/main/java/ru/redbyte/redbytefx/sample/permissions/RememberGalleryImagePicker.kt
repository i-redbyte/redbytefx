package ru.redbyte.redbytefx.sample.permissions

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Opens the gallery for a single image. On API 33+ uses the system photo picker (no permission).
 * On older releases requests read access when needed, then [ActivityResultContracts.GetContent].
 */
@Composable
internal fun rememberGalleryImagePicker(
    onImagePicked: (Uri) -> Unit,
    onAccessDenied: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val sdk = remember { Build.VERSION.SDK_INT }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri) else onAccessDenied()
    }
    val legacyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImagePicked(uri) else onAccessDenied()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val names = readImagePermissionNames(sdk)
        val granted = names.isEmpty() || names.all { result[it] == true }
        if (granted) {
            legacyPicker.launch("image/*")
        } else {
            onAccessDenied()
        }
    }

    return remember(photoPicker, legacyPicker, permissionLauncher, context, sdk) {
        {
            when {
                usesSystemPhotoPicker(sdk) -> {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                hasReadImageAccess(context, sdk) -> legacyPicker.launch("image/*")
                else -> {
                    val names = readImagePermissionNames(sdk)
                    if (names.isEmpty()) {
                        legacyPicker.launch("image/*")
                    } else {
                        permissionLauncher.launch(names.toTypedArray())
                    }
                }
            }
        }
    }
}
