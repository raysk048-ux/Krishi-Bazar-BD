package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.example.data.AppPreferences
import com.example.data.FirestoreKrishiRepository
import com.example.firebase.attemptAutoSignIn
import com.example.navigation.KrishiApp
import com.example.ui.theme.KrishiBazarTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = AppPreferences(applicationContext)
        val firestoreDatabaseId = applicationContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(firestoreDatabaseId)
        val repository = FirestoreKrishiRepository(firestore, prefs)
        val credentialManager = CredentialManager.create(this)

        attemptAutoSignIn(
            context = this,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = lifecycleScope
        )

        setContent {
            KrishiBazarTheme {
                KrishiApp(
                    repository = repository,
                    prefs = prefs,
                    credentialManager = credentialManager
                )
            }
        }
    }
}
