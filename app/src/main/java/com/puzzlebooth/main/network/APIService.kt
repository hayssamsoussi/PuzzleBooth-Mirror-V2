package com.puzzlebooth.server.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class Design(
    val creation_date: String,
    val filename: String,
    val url: String,
    val isLocal: Boolean = false
)

data class Event(
    val id: Int,
    var date: String,
    var names: String,
    var location: String,
    var start_time: String,
    var packages: String,
    var extra_notes: String,
    var instagrams: String,
    var phone: String,
    var is_jemle: Int,
    var total: Int,
    var paid: Int,
    var employees: String,
    var machines: String,
    var departure_time: String,
    var prep_start_time: String,
    var jemle_customer: Int,
    var design_url: String,
    var deposit_info: String,
    var status: Int, // 0 coming / 1 done / 2 cancelled
    var location_maps: String,
    var deposit_withdrawn: Int? // 0 no / 1 yes
)

interface APIService {

    companion object {
        const val REMOTE_BASE_URL = "https://www.puzzleslb.com/"
    }

    @GET("admin/public/api/events/{eventId}")
    fun getEvent(@Path("eventId") id: Int): Observable<Event>

    @GET("puzzlebooth/api_v2/rest/list-designs.php")
    fun listDesigns(): Observable<List<Design>>

    @GET("puzzlebooth/api_v2/rest/list-mosaic.php")
    fun listMosaic(): Observable<List<Design>>
}

object RetrofitInstance {
    var retrofit: Retrofit? = null
    var client: OkHttpClient? = null
    var interceptor: HttpLoggingInterceptor? = null
    var gson: Gson? = null

    const val BASE_URL = "https://jsonplaceholder.typicode.com";

    fun customGsonConverter(): Converter.Factory {
        if(gson == null) {
            gson = GsonBuilder()
                .setLenient()
                //.registerTypeAdapter(EventStatus::class.java, EventStatusTypeAdapter())
                .create()
        }
        return GsonConverterFactory.create(gson)
    }

    fun getRetrofitInstance(): Retrofit {

        if(interceptor == null) {
            interceptor = HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }
        }

        if(client == null) {
            client = OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    val originalRequest = chain.request()
                    val requestWithUserAgent = originalRequest.newBuilder()
                        .header("User-Agent", "My Agent is so cool")
                        .build()
                    chain.proceed(requestWithUserAgent)
                })
                .addInterceptor(Interceptor {
                        chain ->
                    val url = chain
                        .request()
                        .url
                        .newBuilder()
                        .addQueryParameter("unused", "${System.currentTimeMillis()}")
                        .build()
                    chain.proceed(chain.request().newBuilder().url(url).build())
                })
                .addInterceptor(interceptor!!)
                .build()
        }

        if(retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(APIService.REMOTE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

            RxJavaPlugins.setErrorHandler { throwable: Throwable ->
                Log.e(
                    "error",
                    throwable.message!!
                )
            }
        }

        return retrofit!!
    }
}