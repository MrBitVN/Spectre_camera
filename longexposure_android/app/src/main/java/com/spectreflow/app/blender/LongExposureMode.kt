package com.spectreflow.app.blender

enum class LongExposureMode(val id: String) {
    AUTO("auto"),
    MOTION_BLUR("motion_blur"),
    LIGHT_TRAILS("light_trails"),
    VANISH("vanish");

    companion object {
        fun fromId(id: String): LongExposureMode =
            entries.find { it.id == id } ?: AUTO
    }
}
