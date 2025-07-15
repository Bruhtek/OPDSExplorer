package com.bruhtek.opdsexplorer.filesystem

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DocumentTreeRepository private constructor(private val context: Context) {
    private val appContext = context.applicationContext

    companion object {
        @SuppressLint("StaticFieldLeak", "We use appContext instead to avoid memory leaks")
        @Volatile
        private var INSTANCE: DocumentTreeRepository? = null

        fun getInstance(context: Context): DocumentTreeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DocumentTreeRepository(context).also { INSTANCE = it }
            }
        }
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "document_tree_pref"
    )

    private object PreferencesKeys {
        val TREE_URI = stringPreferencesKey("tree_uri")
    }

    val treeUriFlow: Flow<String?> = appContext.dataStore.data
        .catch { e ->
            Log.e("DocumentTreeRepository", "Error reading preferences", e)
            emit(emptyPreferences())
        }
        .map { prefs ->
            prefs[PreferencesKeys.TREE_URI]
        }

    suspend fun saveTreeUri(uri: Uri) {
        appContext.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TREE_URI] = uri.toString()
        }
    }

    suspend fun clearTreeUri() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.TREE_URI)
        }
    }

    suspend fun getTreeUri(): String? {
        return appContext.dataStore.data.first()[PreferencesKeys.TREE_URI]
    }

    fun hasValidPermissions(uri: Uri): Boolean {
        return try {
            val documentFile = DocumentFile.fromTreeUri(appContext, uri)
            documentFile?.exists() == true &&
                    documentFile.canWrite() &&
                    documentFile.canRead()
        } catch (e: Exception) {
            false
        }
    }

}