package com.acadex.app.di

import android.content.Context
import androidx.room.Room
import com.acadex.app.data.local.AcadexDatabase
import com.acadex.app.data.local.NoteDao
import com.acadex.app.data.remote.ApiService
import com.acadex.app.data.repository.AuthRepositoryImpl
import com.acadex.app.data.repository.AssignmentRepositoryImpl
import com.acadex.app.data.repository.ExamRepositoryImpl
import com.acadex.app.data.repository.NotesRepositoryImpl
import com.acadex.app.data.repository.PlannerRepositoryImpl
import com.acadex.app.data.repository.ResourceRepositoryImpl
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.domain.repository.AssignmentRepository
import com.acadex.app.domain.repository.ExamRepository
import com.acadex.app.domain.repository.NotesRepository
import com.acadex.app.domain.repository.PlannerRepository
import com.acadex.app.domain.repository.ResourceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository

    @Binds
    @Singleton
    abstract fun bindResourceRepository(impl: ResourceRepositoryImpl): ResourceRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Firebase
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    // Room Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AcadexDatabase {
        return Room.databaseBuilder(
            context,
            AcadexDatabase::class.java,
            "acadex_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: AcadexDatabase): NoteDao = db.noteDao

    // Retrofit & OkHttp
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://acadex-api-mock.requestcatcher.com/") // Template domain for request mapping
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
