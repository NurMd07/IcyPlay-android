package com.app.icyplay

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object IcyplayContract {
    object GameEntry : BaseColumns {
        const val TABLE_NAME = "game_info"
        const val COLUMN_NAME_GAME_NAME = "game_name"
        const val COLUMN_NAME_USER_ID = "user_id"
        const val COLUMN_NAME_PASSWORD = "password"
        const val COLUMN_NAME_THEME = "theme"
        const val COLUMN_NAME_LOGO_URL = "logo_url"
        const val COLUMN_NAME_TAGS = "tags"  // Can store as a string, separated by commas
        const val COLUMN_NAME_IS_PINNED = "is_pinned"
        const val COLUMN_NAME_MODIFIED_AT = "modified_at"
        const val COLUMN_NAME_REMOTE_LOGO_URL = "remote_logo_url"
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${GameEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${GameEntry.COLUMN_NAME_GAME_NAME} TEXT," +
                "${GameEntry.COLUMN_NAME_USER_ID} TEXT," +
                "${GameEntry.COLUMN_NAME_PASSWORD} TEXT," +
                "${GameEntry.COLUMN_NAME_THEME} TEXT," +
                "${GameEntry.COLUMN_NAME_LOGO_URL} TEXT," +
                "${GameEntry.COLUMN_NAME_TAGS} TEXT," +
                "${GameEntry.COLUMN_NAME_IS_PINNED} BOOLEAN," +
                "${GameEntry.COLUMN_NAME_MODIFIED_AT} TEXT," +
                "${GameEntry.COLUMN_NAME_REMOTE_LOGO_URL} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${GameEntry.TABLE_NAME}"
}

// Database Helper
class IcyplayDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "GameInfo.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(IcyplayContract.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(IcyplayContract.SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    // Delete a game by ID
    fun deleteGameById(id: String): Int {
        val db = writableDatabase
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id)

        return db.delete(IcyplayContract.GameEntry.TABLE_NAME, selection, selectionArgs)
    }
}
