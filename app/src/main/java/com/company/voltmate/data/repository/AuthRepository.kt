package com.company.voltmate.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.company.voltmate.R
import com.company.voltmate.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun observeAuthState(): Flow<FirebaseUser?>
    fun observeUserProfile(uid: String): Flow<UserProfile?>
    suspend fun resolveUserProfile(uid: String): UserProfile?
    fun getLocalRole(uid: String): String
    fun setLocalRole(uid: String, role: String)
    fun isLocalOnboarded(uid: String): Boolean
    fun setLocalOnboarded(uid: String, onboarded: Boolean)
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser>
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser>
    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<FirebaseUser>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    suspend fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("voltmate_prefs", Context.MODE_PRIVATE)

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override fun getLocalRole(uid: String): String {
        return if (uid.isNotBlank()) prefs.getString("role_$uid", "DRIVER") ?: "DRIVER" else "DRIVER"
    }

    override fun setLocalRole(uid: String, role: String) {
        if (uid.isNotBlank()) prefs.edit().putString("role_$uid", role).apply()
    }

    override fun isLocalOnboarded(uid: String): Boolean {
        return if (uid.isNotBlank()) prefs.getBoolean("onboarded_$uid", false) else false
    }

    override fun setLocalOnboarded(uid: String, onboarded: Boolean) {
        if (uid.isNotBlank()) prefs.edit().putBoolean("onboarded_$uid", onboarded).apply()
    }

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { authState ->
            trySend(authState.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Emit local profile first so UI renders immediately on the first frame
        val localRole = getLocalRole(uid)
        val localOnboarded = isLocalOnboarded(uid)
        trySend(
            UserProfile(
                uid = uid,
                name = currentUser?.displayName ?: "EV Driver",
                email = currentUser?.email ?: "",
                role = localRole,
                onboarded = localOnboarded
            )
        )

        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        setLocalRole(uid, profile.role)
                        setLocalOnboarded(uid, profile.onboarded)
                        trySend(profile)
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun resolveUserProfile(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            withTimeoutOrNull(4000) {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        setLocalRole(uid, profile.role)
                        setLocalOnboarded(uid, profile.onboarded)
                    }
                    profile
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            var actContext = context
            while (actContext is ContextWrapper) {
                if (actContext is Activity) break
                actContext = actContext.baseContext
            }
            val activity = actContext as? Activity ?: throw IllegalStateException("Activity context required for CredentialManager")

            val credentialManager = CredentialManager.create(activity)
            val webClientId = activity.getString(R.string.default_web_client_id)

            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = withTimeoutOrNull(4000) {
                    auth.signInWithCredential(authCredential).await()
                } ?: throw IllegalStateException("Google sign-in timed out. Please check network.")
                val user = authResult.user ?: throw IllegalStateException("Firebase user is null")

                // Non-blocking fire-and-forget sync to Firestore
                val localRole = getLocalRole(user.uid)
                val localOnboarded = isLocalOnboarded(user.uid)
                val profile = UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "EV Driver",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    role = localRole,
                    onboarded = localOnboarded
                )
                try {
                    withTimeoutOrNull(4000) {
                        val doc = firestore.collection("users").document(user.uid).get().await()
                        if (!doc.exists()) {
                            firestore.collection("users").document(user.uid).set(profile).await()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background firestore sync error
                }

                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unrecognized credential type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = withTimeoutOrNull(4000) {
                auth.signInWithEmailAndPassword(email.trim(), pass).await()
            } ?: throw IllegalStateException("Sign in timed out. Please check your network connection.")
            val user = result.user ?: throw IllegalStateException("Firebase user is null")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val result = withTimeoutOrNull(4000) {
                auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            } ?: throw IllegalStateException("Sign up timed out. Please try again.")
            val user = result.user ?: throw IllegalStateException("Firebase user is null")
            val profile = UserProfile(
                uid = user.uid,
                name = name.trim().ifEmpty { "EV Driver" },
                email = user.email ?: email.trim(),
                role = "DRIVER",
                onboarded = false
            )
            setLocalRole(user.uid, "DRIVER")
            setLocalOnboarded(user.uid, false)
            try {
                withTimeoutOrNull(4000) {
                    firestore.collection("users").document(user.uid).set(profile).await()
                }
            } catch (e: Exception) {
                // Non-blocking
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            withTimeoutOrNull(4000) {
                auth.sendPasswordResetEmail(email.trim()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        setLocalRole(profile.uid, profile.role)
        setLocalOnboarded(profile.uid, profile.onboarded)
        return try {
            withTimeoutOrNull(4000) {
                firestore.collection("users").document(profile.uid).set(profile).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit) // Local state is valid
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
