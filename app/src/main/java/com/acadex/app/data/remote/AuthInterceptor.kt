package com.acadex.app.data.remote

import android.util.Log
import com.acadex.app.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiServiceLazy: dagger.Lazy<SupabaseApiService>
) : Interceptor {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Add Authorization header if we have an access token and the request doesn't already have one
        var request = originalRequest
        val token = sessionManager.getAccessToken()
        
        val path = originalRequest.url.encodedPath
        val isAuthRoute = path.contains("/auth/v1/signup") || 
                          path.contains("/auth/v1/token") || 
                          path.contains("/auth/v1/recover")
        
        if (token != null && !isAuthRoute && originalRequest.header("Authorization") == null) {
            request = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        
        val response = chain.proceed(request)
        
        // If we get a 401 (Unauthorized) and we are not already on an auth route, try to refresh
        if (response.code == 401 && !isAuthRoute) {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken != null) {
                synchronized(this) {
                    val currentToken = sessionManager.getAccessToken()
                    val requestToken = request.header("Authorization")?.substringAfter("Bearer ")
                    
                    if (currentToken != requestToken && currentToken != null) {
                        // Token was already refreshed by another concurrent request, retry with the new token
                        response.close()
                        val newRequest = request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                        return chain.proceed(newRequest)
                    }
                    
                    try {
                        Log.i("AuthInterceptor", "Access token expired (401). Attempting token refresh...")
                        val apiService = apiServiceLazy.get()
                        val refreshResponse = kotlinx.coroutines.runBlocking {
                            apiService.refreshSession(
                                apiKey = SUPABASE_API_KEY,
                                body = RefreshSessionRequest(refreshToken)
                            )
                        }
                        
                        val newAccessToken = refreshResponse.accessToken
                        val newRefreshToken = refreshResponse.refreshToken
                        if (newAccessToken.isNotEmpty() && newRefreshToken.isNotEmpty()) {
                            Log.i("AuthInterceptor", "Token refresh successful, saving session.")
                            sessionManager.saveSession(
                                accessToken = newAccessToken,
                                refreshToken = newRefreshToken,
                                userId = refreshResponse.user.id,
                                email = refreshResponse.user.email,
                                name = sessionManager.getUserName() ?: ""
                            )
                            
                            // Retry the original request with the new access token
                            response.close()
                            val newRequest = request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                            return chain.proceed(newRequest)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthInterceptor", "Failed to refresh token", e)
                        // If the refresh token is invalid (HTTP 400/401), we must log out
                        if (e is retrofit2.HttpException && (e.code() == 400 || e.code() == 401)) {
                            Log.w("AuthInterceptor", "Refresh token is invalid or expired. Clearing user session.")
                            sessionManager.clearSession()
                        }
                    }
                }
            }
        }
        
        return response
    }
}
