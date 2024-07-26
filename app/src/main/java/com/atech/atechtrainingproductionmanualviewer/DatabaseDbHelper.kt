package com.atech.atechtrainingproductionmanualviewer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // set database version and file name
    companion object {
        const val DATABASE_NAME = "atech.db"
        const val DATABASE_VERSION = 9
    }

    // Notes Table initialization
    object NotesTable {
        const val TABLE_NAME = "notes"
        const val NOTE_OWNER = "note_owner"
        const val COLUMN_ID = "NID"
        const val COLUMN_DATE = "date"
        const val COLUMN_NOTE = "note"
        const val COLUMN_IMAGE_URI = "uri"
        const val COLUMN_IMAGE_NOTE = "image_comment"
        const val COLUMN_ISSUE = "is_issue"
        const val COLUMN_TRAINER = "trainer"
        const val COLUMN_PAGE = "page"
        // create the variable to create the table in the event it does not exist
        const val SQL_CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $NOTE_OWNER TEXT,$COLUMN_DATE DATE, $COLUMN_NOTE TEXT, $COLUMN_IMAGE_URI TEXT,
                $COLUMN_IMAGE_NOTE TEXT, $COLUMN_ISSUE BOOLEAN, $COLUMN_TRAINER TEXT, $COLUMN_PAGE TEXT)"""
    }

    override fun onCreate(db: SQLiteDatabase) {
        // array of the create table sql statements for all tables in database schema
        arrayOf(NotesTable.SQL_CREATE_TABLE).forEach { createTableSQL -> db.execSQL(createTableSQL) }
    }

    // delete and recreate the tables if the version number of the databases increases
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        arrayOf(NotesTable.TABLE_NAME).forEach { tableName -> db.execSQL("DROP TABLE IF EXISTS $tableName") }
        onCreate(db)
    }
}