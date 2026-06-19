package ru.ugrasu.eljunior.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.ugrasu.eljunior.BuildConfig
import ru.ugrasu.eljunior.data.api.MoodleApi
import ru.ugrasu.eljunior.data.parser.ItportScheduleParser
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: InMemoryCookieJar): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BODY логирует мегабайты base64 из Moodle и сильно тормозит эмулятор
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.MOODLE_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMoodleApi(retrofit: Retrofit): MoodleApi {
        return retrofit.create(MoodleApi::class.java)
    }

    @Provides
    @Singleton
    fun provideItportScheduleParser(): ItportScheduleParser {
        return ItportScheduleParser()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
