package com.app.str.di

import android.content.Context
import com.app.str.data.api.AttendanceApiService
import com.app.str.data.api.AuthApiService
import com.app.str.data.api.DailyReportApiService
import com.app.str.data.api.ForgotPasswordApiService
import com.app.str.data.api.HourlyReportApiService
import com.app.str.data.api.IncentiveApiService
import com.app.str.data.api.NotificationApiService
import com.app.str.data.api.ProfileApiService
import com.app.str.data.api.SalarySlipApiService
import com.app.str.data.api.WorkPlanApiService
import com.app.str.network.AuthInterceptor
import com.app.str.utils.TokenAuthenticator
import com.app.str.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegularOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "https://shubhamgharde29.pythonanywhere.com/api/"
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    // OkHttpClient for auth API (no authenticator to avoid circular dependency)
    @Provides
    @Singleton
    @AuthOkHttpClient
    fun provideAuthOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Regular OkHttpClient with authenticator for other APIs
    @Provides
    @Singleton
    @RegularOkHttpClient
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)  // Add authenticator for automatic token refresh
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Retrofit for auth API
    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(@AuthOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Regular Retrofit for other APIs
    @Provides
    @Singleton
    fun provideRetrofit(@RegularOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApiService(@AuthRetrofit retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAttendanceApiService(retrofit: Retrofit): AttendanceApiService {
        return retrofit.create(AttendanceApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideWorkPlanApiService(retrofit: Retrofit): WorkPlanApiService {
        return retrofit.create(WorkPlanApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDailyReportApiService(retrofit: Retrofit): DailyReportApiService {
        return retrofit.create(DailyReportApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideProfileApiService(retrofit: Retrofit): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideSalarySlipApiService(retrofit: Retrofit): SalarySlipApiService {
        return retrofit.create(SalarySlipApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideHourlyReportApiService(retrofit: Retrofit): HourlyReportApiService {
        return retrofit.create(HourlyReportApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideIncentiveApiService(retrofit: Retrofit): IncentiveApiService {
        return retrofit.create(IncentiveApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService {
        return retrofit.create(NotificationApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideForgotPasswordApiService(@AuthRetrofit retrofit: Retrofit): ForgotPasswordApiService {
        return retrofit.create(ForgotPasswordApiService::class.java)
    }
}
