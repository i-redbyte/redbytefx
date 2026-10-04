package ru.redbyte.redbytefx.sample.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryImageAccessTest {

    @Test
    fun api33UsesPhotoPickerWithoutLegacyStorage() {
        assertTrue(usesSystemPhotoPicker(33))
        assertEquals(listOf("android.permission.READ_MEDIA_IMAGES"), readImagePermissionNames(33))
    }

    @Test
    fun api29Through32NeedsNoRuntimePermissionForPicker() {
        assertFalse(usesSystemPhotoPicker(32))
        assertEquals(emptyList<String>(), readImagePermissionNames(29))
        assertEquals(emptyList<String>(), readImagePermissionNames(32))
    }

    @Test
    fun api28RequestsLegacyReadStorage() {
        assertFalse(usesSystemPhotoPicker(28))
        assertEquals(listOf("android.permission.READ_EXTERNAL_STORAGE"), readImagePermissionNames(28))
    }
}
