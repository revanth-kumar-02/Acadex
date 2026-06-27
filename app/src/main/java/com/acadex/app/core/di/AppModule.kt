package com.acadex.app.core.di

import android.content.Context
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.data.repositories.*
import com.acadex.app.core.network.AuthInterceptor
import com.acadex.app.core.constants.AppConstants
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindAssignmentRepository(impl: AssignmentRepositoryImpl): AssignmentRepository

    @Binds
    @Singleton
    abstract fun bindPlannerRepository(impl: PlannerRepositoryImpl): PlannerRepository

    @Binds
    @Singleton
    abstract fun bindAnnouncementRepository(impl: AnnouncementRepositoryImpl): AnnouncementRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Retrofit & OkHttp
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConstants.SUPABASE_BASE_URL) // Acadex Supabase Project
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSupabaseApiService(retrofit: Retrofit): SupabaseApiService {
        return retrofit.create(SupabaseApiService::class.java)
    }
}
