package pan.alexander.tordnscrypt.appextension

import android.content.*
import android.database.Cursor
import android.net.Uri

class ExtensionContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        context?.contentResolver?.notifyChange(uri, null)
        return uri
    }

    override fun getType(uri: Uri): String? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    companion object {
        private const val PREFERENCE_AUTHORITY = "com.fulldive.tordnscrypt"
        const val BASE_URL = "content://$PREFERENCE_AUTHORITY"
        const val WORK_STATUS = "WORK_STATUS"
    }
}

fun getContentUri(value: String): Uri {
    return Uri
        .parse(ExtensionContentProvider.BASE_URL)
        .buildUpon().appendPath(ExtensionContentProvider.WORK_STATUS)
        .appendPath(value)
        .build()
}