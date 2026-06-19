package ru.ugrasu.eljunior.util

import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserMessage(): String {
    return when (this) {
        is UnknownHostException -> "Нет подключения к интернету или не работает DNS"
        is SocketTimeoutException -> "Превышено время ожидания ответа сервера"
        else -> localizedMessage ?: "Неизвестная ошибка"
    }
}

