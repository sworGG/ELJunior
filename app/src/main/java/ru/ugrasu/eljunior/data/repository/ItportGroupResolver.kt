package ru.ugrasu.eljunior.data.repository

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Извлекает ID учебной группы из URL и HTML страниц itport.
 */
object ItportGroupResolver {

    private val GROUP_ID_IN_URL = Regex("""/timetable/group/(\d+)""")
    private val GROUP_ID_IN_TEXT = Regex("""timetable/group/(\d+)""")

    fun fromUrl(url: String): Int? {
        return GROUP_ID_IN_URL.find(url)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun fromHtml(html: String): Int? {
        return fromPage(html, "")
    }

    /**
     * Извлекает ID группы из HTML и финального URL страницы (после редиректов).
     */
    fun fromPage(html: String, finalUrl: String): Int? {
        if (html.isBlank() || isLoginPage(html)) return null

        fromUrl(finalUrl)?.let { return it }
        fromMetaRefresh(html)?.let { return it }
        fromUrl(html)?.let { return it }

        GROUP_ID_IN_TEXT.findAll(html)
            .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
            .firstOrNull()
            ?.let { return it }

        val doc = Jsoup.parse(html)
        fromSelectedOption(doc)?.let { return it }

        if (!isMultiGroupSearchPage(finalUrl, html)) {
            return parseFromDocument(doc)
        }

        return null
    }

    fun fromHtmlByGroupName(html: String, groupName: String): Int? {
        if (html.isBlank() || groupName.isBlank()) return null

        val doc = Jsoup.parse(html)
        val normalizedName = groupName.trim()

        doc.select("a[href*=/timetable/group/]").forEach { link ->
            val href = link.attr("href")
            val groupId = fromUrl(href) ?: return@forEach
            val linkText = link.text().trim()
            if (linkText.contains(normalizedName, ignoreCase = true)) {
                return groupId
            }
        }

        doc.select(".tableView-group, .group-name, [data-group-name]").forEach { element ->
            if (element.text().contains(normalizedName, ignoreCase = true)) {
                val nearbyLink = element.selectFirst("a[href*=/timetable/group/]")
                    ?: element.parent()?.selectFirst("a[href*=/timetable/group/]")
                nearbyLink?.attr("href")?.let { href ->
                    fromUrl(href)?.let { return it }
                }
            }
        }

        return null
    }

    fun extractGroupName(html: String): String? {
        if (html.isBlank() || isLoginPage(html)) return null

        val doc = Jsoup.parse(html)
        val selectors = listOf(
            ".timetable-group-name",
            ".group-title",
            "h1 .group-name",
            "h2 .group-name",
            ".page-title"
        )

        selectors.forEach { selector ->
            val text = doc.selectFirst(selector)?.text()?.trim()
            if (!text.isNullOrBlank() && text.length in 3..20) {
                return text
            }
        }

        doc.select("a[href*=/timetable/group/]").firstOrNull()?.text()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return null
    }

    private fun parseFromDocument(doc: Document): Int? {
        doc.select("a[href*=/timetable/group/]").forEach { link ->
            fromUrl(link.attr("href"))?.let { return it }
        }

        doc.select("option[value]").forEach { option ->
            val value = option.attr("value")
            value.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
            fromUrl(value)?.let { return it }
        }

        return null
    }

    private fun fromMetaRefresh(html: String): Int? {
        val metaRefresh = Regex(
            """<meta[^>]+http-equiv=["']?refresh["']?[^>]+content=["']?\d+\s*;\s*url=['"]?([^'">\s]+)""",
            RegexOption.IGNORE_CASE
        ).find(html)
        metaRefresh?.groupValues?.getOrNull(1)?.let { redirectUrl ->
            fromUrl(redirectUrl)?.let { return it }
        }
        return null
    }

    private fun fromSelectedOption(doc: Document): Int? {
        val selectors = listOf(
            "select[name*=group] option[selected]",
            "select[id*=group] option[selected]",
            "option[selected][value]"
        )
        selectors.forEach { selector ->
            doc.select(selector).forEach { option ->
                val value = option.attr("value")
                value.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
                fromUrl(value)?.let { return it }
            }
        }
        return null
    }

    private fun isMultiGroupSearchPage(finalUrl: String, html: String): Boolean {
        return finalUrl.contains("/timetable/search") ||
            html.contains("/timetable/search") ||
            Jsoup.parse(html).select("a[href*=/timetable/group/]").size > 3
    }

    private fun isLoginPage(html: String): Boolean {
        return html.contains("kt_login_signin_form") ||
            html.contains("Elios 2.0 - Авторизация")
    }
}
