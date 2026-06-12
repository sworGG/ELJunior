package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

data class ItportLoginRequest(
    val email: String,
    val password: String,
    val form: Int = 0,
    val group: String = "",
    val ffullname: String = "",
    val bithday: String = "",
    val passport: String = "",
    val snils: String = "",
    val inn: String = ""
)

data class ItportLoginResponse(
    @SerializedName("redirect") val redirect: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("message") val message: String?
)
