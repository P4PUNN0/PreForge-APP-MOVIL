package com.example.preforge

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun rememberGoogleSignInLauncher(
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    if (LocalInspectionMode.current) return {}

    val firebaseAuth = remember {
        try {
            Firebase.auth
        } catch (exception: Exception) {
            null
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            onError("Inicio de sesión cancelado")
            return@rememberLauncherForActivityResult
        }

        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
                ?: throw IllegalStateException("No se recibió la cuenta de Google")
            val idToken = account.idToken
                ?: throw IllegalStateException("La cuenta de Google no devolvió un token")

            if (firebaseAuth == null) {
                onError("Firebase Auth no está inicializado")
                return@rememberLauncherForActivityResult
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    val user = firebaseAuth.currentUser
                    if (task.isSuccessful && user != null) {
                        onSuccess(user)
                    } else {
                        onError(
                            task.exception?.localizedMessage
                                ?: "No se pudo iniciar sesión con Google"
                        )
                    }
                }
        } catch (exception: ApiException) {
            onError("Error de Google (${exception.statusCode}): ${exception.message}")
        } catch (exception: Exception) {
            onError(exception.localizedMessage ?: "Error al iniciar sesión con Google")
        }
    }

    return {
        try {
            val resourceId = resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resourceId == 0) {
                onError("Falta default_web_client_id. Sincroniza el proyecto con Firebase.")
            } else {
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(resources.getString(resourceId))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, options)
                client.signOut().addOnCompleteListener {
                    launcher.launch(client.signInIntent)
                }
            }
        } catch (exception: Exception) {
            onError(exception.localizedMessage ?: "No se pudo abrir Google Sign-In")
        }
    }
}
