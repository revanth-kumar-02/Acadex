package com.acadex.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.acadex.app.data.repositories.AuthRepository
import com.acadex.app.core.navigation.NavGraph
import com.acadex.app.ui.theme.AcadexTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            Log.d("MainActivity", "Running in DEBUG mode. Screenshot protection disabled.")
        } else {
            Log.d("MainActivity", "Running in RELEASE mode.")
        }
        
        Log.d("MainActivity", "onCreate called, checking intent for deep links")
        intent?.data?.let { handleDeepLink(it) }

        setContent {
            AcadexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent called, checking intent data")
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: Uri) {
        Log.i("MainActivity", "Received deep link: $uri")
        if (uri.scheme == "acadex" && uri.host == "auth" && uri.path == "/callback") {
            // Fragment usually contains access_token and refresh_token
            val fragment = uri.fragment
            val params = parseFragment(fragment)
            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]
            val type = params["type"] ?: "signup"

            Log.d("MainActivity", "Parsed parameters - type: $type, access_token: ${accessToken?.take(10)}..., refresh_token: ${refreshToken?.take(5)}...")

            if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val result = authRepository.handleDeepLinkSession(accessToken, refreshToken)
                    if (result.isSuccess) {
                        Log.i("MainActivity", "Deep link session handling successfully authenticated user. Transitioning state.")
                    } else {
                        Log.e("MainActivity", "Failed to handle deep link session token exchange", result.exceptionOrNull())
                    }
                }
            } else {
                Log.w("MainActivity", "Deep link received but access_token or refresh_token is missing")
            }
        }
    }

    private fun parseFragment(fragment: String?): Map<String, String> {
        if (fragment.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val pairs = fragment.split("&")
            for (pair in pairs) {
                val parts = pair.split("=")
                if (parts.size == 2) {
                    map[parts[0]] = parts[1]
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error parsing deep link URI fragment", e)
        }
        return map
    }
}
