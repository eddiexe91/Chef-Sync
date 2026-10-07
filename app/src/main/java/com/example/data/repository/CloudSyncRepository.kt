package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class CloudLog(
    val action: String = "",
    val status: String = "",
    val message: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

class CloudSyncRepository(context: Context) {
    private val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    private val auth = Firebase.auth

    private fun requireUserId(): String = auth.currentUser?.uid ?: error("User must be signed in")

    fun observeSpreadsheetId(): Flow<String?> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        return db.collection("users").document(uid).snapshots().map { snapshot ->
            snapshot.getString("spreadsheetId")
        }
    }

    suspend fun updateSpreadsheetId(spreadsheetId: String) {
        val uid = requireUserId()
        db.collection("users").document(uid).set(
            mapOf("spreadsheetId" to spreadsheetId),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun addLog(action: String, status: String, message: String) {
        val uid = auth.currentUser?.uid ?: return
        val log = mapOf(
            "action" to action,
            "status" to status,
            "message" to message,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("logs").add(log).await()
    }
}
