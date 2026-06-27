package com.acadex.app.di

import android.content.Context
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.data.repository.AuthRepositoryImpl
import com.acadex.app.data.repository.AssignmentRepositoryImpl
import com.acadex.app.data.repository.NotesRepositoryImpl
import com.acadex.app.data.repository.PlannerRepositoryImpl
import com.acadex.app.data.repository.AnnouncementRepositoryImpl
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.domain.repository.AssignmentRepository
import com.acadex.app.domain.repository.NotesRepository
import com.acadex.app.domain.repository.PlannerRepository
import com.acadex.app.domain.repository.AnnouncementRepository
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
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Retrofit & OkHttp
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: com.acadex.app.data.remote.AuthInterceptor
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
            .baseUrl("https://ombpzrctfsqlpaxbvjha.supabase.co/") // Acadex Supabase Project
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
