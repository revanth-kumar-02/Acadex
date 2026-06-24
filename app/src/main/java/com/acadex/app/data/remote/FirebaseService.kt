package com.acadex.app.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    val auth: FirebaseAuth,
    val firestore: FirebaseFirestore,
    val storage: FirebaseStorage
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Storage Upload Helpers
    suspend fun uploadFile(path: String, inputStream: InputStream): String {
        val ref = storage.reference.child(path)
        ref.putStream(inputStream).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteFile(path: String) {
        storage.reference.child(path).delete().await()
    }
}
