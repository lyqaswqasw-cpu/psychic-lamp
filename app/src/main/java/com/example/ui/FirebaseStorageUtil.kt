package com.example.ui

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseStorageUtil {
    suspend fun uploadFile(uri: Uri, folder: String): String? {
        return try {
            val storage = FirebaseStorage.getInstance()
            val ref = storage.reference.child("$folder/${UUID.randomUUID()}")
            val uploadTask = ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
