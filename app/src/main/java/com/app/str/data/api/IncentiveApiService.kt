package com.app.str.data.api

import com.app.str.data.model.IncentiveResponse
import retrofit2.Response
import retrofit2.http.GET

interface IncentiveApiService {
    
    @GET("my-incentives/")
    suspend fun getMyIncentives(): Response<List<IncentiveResponse>>
}
