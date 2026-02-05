package com.app.str.data.api

import com.app.str.data.model.SalarySlipResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SalarySlipApiService {
    
    @GET("user-salary-slip/slip/")
    suspend fun getSalarySlip(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<SalarySlipResponse>
}