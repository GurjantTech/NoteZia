package com.app.data.local

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

/**
 * Prepares the on-disk database file for plain SQLite + Room.
 *
 * Legacy builds that used SQLCipher store data in a file that does not begin with the standard
 * SQLite 3 magic header. Room cannot open those files; attempting to do so can fail or crash.
 * If the file at [DATABASE_NAME] exists and is not a plain SQLite database, it is deleted so
 * Room can create a new one.
 *
 * Plain SQLite databases (including all current unencrypted Room DBs) are left unchanged.
 *
 * **Data loss:** Notes inside an encrypted file cannot be recovered without SQLCipher and the
 * original passphrase. Those users will get an empty database after upgrade.
 */
object NoteDatabaseFiles {
    const val DATABASE_NAME = "notezy_db"

    private val SQLITE3_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun preparePlainSqliteDatabase(context: Context) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return

        val len = dbFile.length()
        if (len == 0L) {
            context.deleteDatabase(DATABASE_NAME)
            return
        }
        if (len < SQLITE3_MAGIC.size) {
            context.deleteDatabase(DATABASE_NAME)
            return
        }

        val headerMatches = try {
            fileStartsWith(dbFile, SQLITE3_MAGIC)
        } catch (_: Exception) {
            return
        }

        if (!headerMatches) {
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private fun fileStartsWith(file: File, prefix: ByteArray): Boolean {
        RandomAccessFile(file, "r").use { raf ->
            val buf = ByteArray(prefix.size)
            raf.readFully(buf)
            return buf.contentEquals(prefix)
        }
    }
}
