package com.app.str.data.api

import com.app.str.data.model.HourlyCheckResponse
import retrofit2.Response
import retrofit2.http.GET

interface HourlyReportApiService {
    
    @GET("simple-hourly-check/")
    suspend fun checkHourlyReport(): Response<HourlyCheckResponse>
}
