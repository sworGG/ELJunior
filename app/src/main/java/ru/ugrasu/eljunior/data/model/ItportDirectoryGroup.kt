package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

data class ItportDirectoryGroup(
    @SerializedName("name") val name: String?,
    @SerializedName("number") val number: String?,
    @SerializedName("groupOid") val groupOid: Int?
)
