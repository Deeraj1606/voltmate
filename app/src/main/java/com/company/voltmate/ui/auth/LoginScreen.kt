package com.company.voltmate.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.R
import com.company.voltmate.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = InkPrimary,
        unfocusedTextColor = InkPrimary,
        focusedLabelColor = PrimaryGreen,
        unfocusedLabelColor = InkSecondary,
        focusedPlaceholderColor = Color(0xFF6B7280),
        unfocusedPlaceholderColor = Color(0xFF6B7280),
        focusedBorderColor = PrimaryGreen,
        unfocusedBorderColor = BorderHairline,
        focusedContainerColor = WhiteCanvas,
        unfocusedContainerColor = WhiteCanvas,
        cursorColor = PrimaryGreen
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = DeepGreen,
                        contentColor = WhiteCanvas,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        },
        containerColor = WhiteCanvas,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            VoltMateLogo(size = 72.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "VOLTMATE",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.isSignUpMode) "Create your account" else "Welcome to VoltMate",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )

            Text(
                text = if (uiState.isSignUpMode) {
                    "Join the peer-to-peer EV charging community"
                } else {
                    "Sign in to access community chargers & fast charging"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Google Sign In Button
            val googleEnabled = !uiState.isLoading && !isGoogleLoading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(SurfaceBg, RoundedCornerShape(28.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(28.dp))
                    .clickable(enabled = googleEnabled) {
                        scope.launch {
                            isGoogleLoading = true
                            try {
                                var actContext: Context = context
                                while (actContext is ContextWrapper) {
                                    if (actContext is Activity) break
                                    actContext = actContext.baseContext
                                }
                                val activity = actContext as? Activity
                                    ?: throw IllegalStateException("Activity context required for CredentialManager")

                                val webClientId = activity.getString(R.string.default_web_client_id)
                                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(webClientId)
                                    .setAutoSelectEnabled(false)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(signInWithGoogleOption)
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = CredentialManager.create(activity).getCredential(activity, request)
                                val cred = result.credential

                                if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(cred.data)
                                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                    val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                                    val user = authResult.user
                                    if (user != null) {
                                        onAuthSuccess()
                                    }
                                } else {
                                    val msg = "Google sign-in failed: Unrecognized credential type"
                                    Log.e("VoltMateAuth", msg)
                                    snackbarHostState.showSnackbar(msg)
                                }
                            } catch (e: GetCredentialCancellationException) {
                                val msg = "Cancelled: ${e.message ?: "Sign-in dismissed. If it closed immediately, add debug SHA-1 to Firebase Console."}"
                                Log.e("VoltMateAuth", msg, e)
                                snackbarHostState.showSnackbar(msg)
                            } catch (e: NoCredentialException) {
                                val msg = "Google sign-in failed: NoCredentialException: Add a Google account on this device"
                                Log.e("VoltMateAuth", msg, e)
                                snackbarHostState.showSnackbar(msg)
                            } catch (e: GetCredentialException) {
                                val msg = "Google sign-in failed: ${e.javaClass.simpleName}: ${e.message}"
                                Log.e("VoltMateAuth", msg, e)
                                snackbarHostState.showSnackbar(msg)
                            } catch (e: Exception) {
                                val msg = "Google sign-in failed: ${e.javaClass.simpleName}: ${e.message}"
                                Log.e("VoltMateAuth", msg, e)
                                snackbarHostState.showSnackbar(msg)
                            } finally {
                                isGoogleLoading = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryGreen,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(WhiteCanvas, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = InkPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderHairline)
                Text(
                    text = "OR EMAIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderHairline)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inputs
            AnimatedVisibility(visible = uiState.isSignUpMode) {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = InkSecondary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = InkSecondary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = InkSecondary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = InkSecondary
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.isSignUpMode) {
                            viewModel.signUpWithEmail(email, password, name)
                        } else {
                            viewModel.signInWithEmail(email, password)
                        }
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (!uiState.isSignUpMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.forgotPassword(email) },
                        enabled = !uiState.isLoading && !isGoogleLoading
                    ) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryGreen
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary 56dp Pill Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (uiState.isSignUpMode) {
                        viewModel.signUpWithEmail(email, password, name)
                    } else {
                        viewModel.signInWithEmail(email, password)
                    }
                },
                enabled = !uiState.isLoading && !isGoogleLoading,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = WhiteCanvas
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = WhiteCanvas,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = if (uiState.isSignUpMode) "Create Account" else "Sign In",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WhiteCanvas
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Sign-in / Sign-up
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isSignUpMode) "Already have an account?" else "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                TextButton(
                    onClick = { viewModel.toggleSignUpMode() },
                    enabled = !uiState.isLoading && !isGoogleLoading
                ) {
                    Text(
                        text = if (uiState.isSignUpMode) "Sign In" else "Sign Up",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
