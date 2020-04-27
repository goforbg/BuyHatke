package com.androar.buyhatke

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface API {
    @GET("getCoupons.php?pos=1")
    fun getDiscounts(): Call<String>?
}