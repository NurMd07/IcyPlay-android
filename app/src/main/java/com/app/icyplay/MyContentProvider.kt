package com.app.icyplay

import android.content.ContentProvider
import android.content.UriMatcher
import android.net.Uri
import android.util.Log
import java.io.File

abstract class MyContentProvider : ContentProvider() {

    private lateinit var uriMatcher: UriMatcher

    companion object {
        const val IMAGES = 1
    }

    override fun onCreate(): Boolean {
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI("com.app.icyplay.provider", "images/*", IMAGES)
        }
        return true
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return when (uriMatcher.match(uri)) {
            IMAGES -> {
                val filePath = getFilePath(uri)
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                    1 // One file deleted
                } else {
                    0 // No files deleted
                }
            }
            else -> throw IllegalArgumentException("Unsupported URI: $uri")
        }.also {
            context?.contentResolver?.notifyChange(uri, null)
        }
    }

    private fun getFilePath(uri: Uri): String {

        return uri.path ?: throw IllegalArgumentException("Invalid URI")
    }


}