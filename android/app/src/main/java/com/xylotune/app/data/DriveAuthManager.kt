package com.xylotune.app.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

class DriveAuthCancelledException : Exception("Google sign-in was cancelled")

/**
 * Wraps Identity.getAuthorizationClient — the current, non-deprecated way to get an OAuth
 * token for one scope on Android. Deliberately different from (and simpler than) the web's
 * pasted-Client-ID Google Identity Services flow: no client ID string lives in this app at
 * all. Google resolves the request via this app's package name + signing SHA-1 fingerprint,
 * registered as an Android-type OAuth client in the same Cloud Console project the web
 * app's Client ID already belongs to (get the debug SHA-1 via `./gradlew signingReport`).
 */
class DriveAuthManager(private val activity: Activity) {
    // Wired by rememberDriveAuthManager() right after construction, mirroring PadState's
    // onBeforeEdit — the launcher can only be created via rememberLauncherForActivityResult
    // inside a @Composable, so it can't be a constructor parameter here.
    lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>

    private var pendingResolution: CompletableDeferred<Intent?>? = null

    fun onResolutionResult(intent: Intent?) {
        pendingResolution?.complete(intent)
        pendingResolution = null
    }

    /**
     * Never shows any UI. Returns a token if the drive.file scope grant already exists
     * silently (e.g. from a previous session), or null otherwise — the caller's own
     * "was connected before" flag decides whether it's worth calling this at all.
     */
    suspend fun trySilent(): String? {
        val result = requestAuthorization()
        return if (result.hasResolution()) null else result.accessToken
    }

    /** May show a consent screen. Throws [DriveAuthCancelledException] if the user backs out. */
    suspend fun authorizeInteractive(): String {
        val result = requestAuthorization()
        if (!result.hasResolution()) {
            return result.accessToken ?: error("Authorization succeeded with no access token")
        }
        val pendingIntent = result.pendingIntent
            ?: error("Authorization needs a resolution but provided no PendingIntent")
        val deferred = CompletableDeferred<Intent?>()
        pendingResolution = deferred
        launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        val resultIntent = deferred.await() ?: throw DriveAuthCancelledException()
        val resolved = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(resultIntent)
        return resolved.accessToken ?: error("Authorization succeeded with no access token")
    }

    private suspend fun requestAuthorization(): AuthorizationResult =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            Identity.getAuthorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No Activity found in this Context chain")
}

@Composable
fun rememberDriveAuthManager(): DriveAuthManager {
    val activity = LocalContext.current.findActivity()
    val manager = remember { DriveAuthManager(activity) }
    manager.launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        manager.onResolutionResult(if (result.resultCode == Activity.RESULT_OK) result.data else null)
    }
    return manager
}
