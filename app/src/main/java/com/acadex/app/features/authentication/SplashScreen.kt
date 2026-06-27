package com.acadex.app.features.authentication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadex.app.ui.components.GlassyCard
import com.acadex.app.ui.theme.Accent
import com.acadex.app.ui.theme.BrandGradient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        Log.d("SplashScreen", "Auth state collected: $authState")
        if (hasNavigated) return@LaunchedEffect

        when (authState) {
            is AuthState.Authenticated -> {
                Log.d("SplashScreen", "Authenticated user found, navigating to Home")
                hasNavigated = true
                onNavigateToHome()
            }
            is AuthState.Unauthenticated -> {
                Log.d("SplashScreen", "Unauthenticated session, navigating to Login")
                hasNavigated = true
                onNavigateToLogin()
            }
            else -> {
                Log.d("SplashScreen", "Loading / Idle state: keeping splash visible")
            }
        }
    }

    // Safeguard: Timeout in 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        if (!hasNavigated) {
            Log.d("SplashScreen", "Timeout (2s) reached. Resolving manually. Current state: $authState")
            val user = viewModel.currentUser.value
            hasNavigated = true
            if (user != null) {
                Log.d("SplashScreen", "Cached user session found via fallback, navigating to Home")
                onNavigateToHome()
            } else {
                Log.d("SplashScreen", "No user session found, forcing redirect to Login")
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(BrandGradient)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // App branding
            Text(
                text = "Acadex",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = Color.White
                )
            )
            Text(
                text = "Academic Operating System",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            GlassyCard(
                modifier = Modifier.fillMaxWidth(0.8f),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Accent,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Optimizing workspace...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
