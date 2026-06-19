package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

data class EliosNotification(
    val id: Int,
    val title: String,
    val subtitle: String,
    val sentAt: String,
    val isRead: Boolean,
    val typeId: Int
)

data class ItportNotificationDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("sended_at") val sentAt: String? = null,
    @SerializedName("readed_at") val readAt: String? = null,
    @SerializedName("type_id") val typeId: Int? = null,
    @SerializedName("is_system") val isSystem: Int? = null
) {
    fun toDomain(): EliosNotification {
        return EliosNotification(
            id = id,
            title = title.orEmpty().ifBlank { "Уведомление" },
            subtitle = subtitle.orEmpty(),
            sentAt = sentAt.orEmpty(),
            isRead = !readAt.isNullOrBlank(),
            typeId = typeId ?: 0
        )
    }
}

fun List<ItportNotificationDto>.toDomainNotifications(): List<EliosNotification> {
    return filter { dto ->
        val typeId = dto.typeId ?: 0
        typeId !in EXCLUDED_TYPE_IDS
    }.map { it.toDomain() }
}

private val EXCLUDED_TYPE_IDS = setOf(33, 34)
