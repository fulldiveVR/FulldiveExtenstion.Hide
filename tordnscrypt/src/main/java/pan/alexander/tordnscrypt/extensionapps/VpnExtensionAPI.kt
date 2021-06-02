package pan.alexander.tordnscrypt.extensionapps

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class VpnExtensionAPI : ContentProvider() {

    private val sharedPreferences: SharedPreferences? by lazy {
        context?.getSharedPreferences(
            CONFIGURATION_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE
        )
    }

    private val editor: SharedPreferences.Editor? by lazy {
        sharedPreferences?.edit()
    }

    private val matcher: UriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply { addURI(PREFERENCE_AUTHORITY, "*/*", MATCH_DATA) }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.$PREFERENCE_AUTHORITY$.item"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        values?.valueSet()?.forEach { entry ->
            val key = entry.key
            val value = entry.value
            editor?.apply {
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Long -> putLong(key, value)
                    is Int -> putInt(key, value)
                    is Float -> putFloat(key, value)
                }
                apply()
            }
        }
        context?.contentResolver?.notifyChange(uri, null)
        return uri
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val cursor: MatrixCursor?
        when (matcher.match(uri)) {
            MATCH_DATA -> {
                val key = uri.pathSegments[0]
                val type = uri.pathSegments[1]
                cursor = MatrixCursor(arrayOf(key))
                sharedPreferences?.apply {
                    if (!contains(key)) return cursor
                    val rowBuilder = cursor.newRow()
                    val storedObject = when (type) {
                        STRING_TYPE -> getString(key, null)
                        BOOLEAN_TYPE -> if (getBoolean(key, false)) 1 else 0
                        LONG_TYPE -> getLong(key, 0L)
                        INT_TYPE -> getInt(key, 0)
                        FLOAT_TYPE -> getFloat(key, 0f)
                        else -> throw IllegalArgumentException("Unsupported type $uri")
                    }
                    rowBuilder.add(storedObject)
                }
            }
            else -> throw IllegalArgumentException("Unsupported uri $uri")
        }
        return cursor
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    companion object {
        const val PREFERENCE_AUTHORITY = "com.fulldive.tordnscrypt"
        const val BASE_URL = "content://$PREFERENCE_AUTHORITY"
        const val STRING_TYPE = "string"
        const val WORK_STATUS = "WORK_STATUS"

        private const val CONFIGURATION_PREFERENCE_FILE_NAME = "Config"
        private const val INT_TYPE = "integer"
        private const val LONG_TYPE = "long"
        private const val FLOAT_TYPE = "float"
        private const val BOOLEAN_TYPE = "boolean"

        private const val MATCH_DATA = 0x010000
    }
}

fun getContentUri(key: String, type: String): Uri {
    return Uri.parse(VpnExtensionAPI.BASE_URL).buildUpon().appendPath(key).appendPath(type).build()
}