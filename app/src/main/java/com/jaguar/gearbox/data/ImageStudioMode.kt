package com.jaguar.gearbox.data

/** Only output preferences, never a source URI, image metadata, crop, dimensions or pixels. */
fun validImageStudioMode(values: Map<String, String>): Boolean =
    values.keys == setOf("format", "quality", "aspectLock") &&
        values["format"] in setOf("PNG", "JPEG", "WEBP_LOSSY") &&
        values["quality"]?.let { value ->
            value.toIntOrNull()?.let { it in 1..100 && it.toString() == value }
        } == true && values["aspectLock"] in setOf("true", "false")
