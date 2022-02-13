package de.obscure.webcam.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.obscure.webcam.entity.WeatherStatistics
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit


private const val BASE_URL = "https://inselsberg2.blcode.de/"
//private const val BASE_URL = "http://10.0.2.2:8080/"

private val logging by lazy {
    val instance = HttpLoggingInterceptor()
    instance.setLevel(HttpLoggingInterceptor.Level.BASIC)
}

val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
val bootstrapClient = OkHttpClient.Builder().cache(appCache).build()

val dns = DnsOverHttps.Builder().client(bootstrapClient)
    .url("https://dns.google/dns-query".toHttpUrl())
    .bootstrapDnsHosts(
        InetAddress.getByName("8.8.4.4"),
        InetAddress.getByName("8.8.8.8"))
    .build()

private val httpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .dns(dns)
    .addInterceptor(logging)
    .connectionPool(ConnectionPool(2, 5, TimeUnit.SECONDS))
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(httpClient.build())
    .build()


interface InselsbergApiService {

    @GET("statistics")
    @Headers("Accept: application/json")
    suspend fun loadStatistic(@Query("since") since: Long): Response<WeatherStatistics>

}

object InselsbergApi {
    val retrofitService: InselsbergApiService by lazy {
        retrofit.create(InselsbergApiService::class.java)
    }

    fun imageUrlTabarz(id: Long, size: String): String = "${BASE_URL}statistics/images/${id}?cameraType=webcam_tabarz&size=${size}"
    fun imageUrlWebcam(id: Long, size: String): String = "${BASE_URL}statistics/images/${id}?cameraType=webcam_iberg&size=${size}"
    fun imageUrlPanomax(id: Long, size: String): String = "${BASE_URL}statistics/images/${id}?cameraType=panomax_iberg&size=${size}"
}