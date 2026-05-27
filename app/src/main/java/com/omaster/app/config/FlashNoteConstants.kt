package com.omaster.app.config

object FlashNoteConstants {
    const val ACTION_FLASH_NOTE = "com.coloros.flashnote.action.ADD_NOTE"
    const val ACTION_FLASH_NOTE_V2 = "com.coloros.flashnote.action.V2_ADD_NOTE"
    const val EXTRA_TITLE = "title"
    const val EXTRA_CONTENT = "content"
    const val EXTRA_TAGS = "tags"
    const val EXTRA_SOURCE = "source"
    const val EXTRA_CATEGORY = "category"
    const val EXTRA_TIMESTAMP = "timestamp"
    const val EXTRA_ATTACHMENT_URI = "attachment_uri"
    const val EXTRA_ATTACHMENT_TYPE = "attachment_type"

    const val CATEGORY_PRESET = "preset"
    const val CATEGORY_CAMERA_PARAMS = "camera_params"
    const val CATEGORY_PHOTO_STYLE = "photo_style"

    const val ATTACHMENT_TYPE_IMAGE = "image"
    const val ATTACHMENT_TYPE_VIDEO = "video"
    const val ATTACHMENT_TYPE_TEXT = "text"

    const val SOURCE_OPPO_MASTER = "oppo_master"
    const val SOURCE_PRESET_CLOUD = "preset_cloud"
    const val SOURCE_USER_CREATED = "user_created"

    const val TAG_PHOTO = "photo"
    const val TAG_PRESET = "preset"
    const val TAG_CAMERA = "camera"
    const val TAG_STYLE = "style"

    const val BROADCAST_PERMISSION = "com.coloros.flashnote.permission.WRITE_NOTE"

    const val MIN_API_LEVEL = 26
    const val RECOMMENDED_API_LEVEL = 30

    const val DEFAULT_PRIORITY = 0
    const val MAX_CONTENT_LENGTH = 5000
    const val MAX_TITLE_LENGTH = 200

    const val ENABLED_FEATURES = listOf(
        FEATURE_PRESET_SAVE,
        FEATURE_CAMERA_PARAMS_SAVE,
        FEATURE_IMAGE_ATTACHMENT,
        FEATURE_AUTO_TAG,
        FEATURE_QUICK_EXPORT
    )

    const val FEATURE_PRESET_SAVE = "preset_save"
    const val FEATURE_CAMERA_PARAMS_SAVE = "camera_params_save"
    const val FEATURE_IMAGE_ATTACHMENT = "image_attachment"
    const val FEATURE_AUTO_TAG = "auto_tag"
    const val FEATURE_QUICK_EXPORT = "quick_export"
    const val FEATURE_ONE_TAP_SAVE = "one_tap_save"
    const val FEATURE_SMART_CATEGORY = "smart_category"

    fun isFlashNoteAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= MIN_API_LEVEL
    }

    fun isFullFeatureAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= RECOMMENDED_API_LEVEL
    }
}
