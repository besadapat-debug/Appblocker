package com.example.appblocker

object WebsiteUtils {

    private val DOMAIN_REGEX = Regex("^[a-z0-9-]+(\\.[a-z0-9-]+)+$")
    private val SECOND_LEVEL = setOf("com", "co", "net", "org", "gov", "edu", "ac")

    // What the user typed ("https://www.YouTube.com/watch?v=1") -> "youtube.com".
    // Returns null if it doesn't look like a website.
    fun normalizeDomain(input: String): String? {
        var s = input.trim().lowercase()
        if (s.isEmpty() || s.any { it.isWhitespace() }) return null
        s = s.replace(Regex("^[a-z][a-z0-9+.-]*://"), "")
        s = s.substringBefore('/').substringBefore('?').substringBefore('#')
        s = s.substringAfterLast('@').substringBefore(':')
        s = s.removePrefix("www.").trimEnd('.')
        return if (DOMAIN_REGEX.matches(s)) s else null
    }

    // Text from a browser's address bar -> host, or null if it's a search
    // query / empty / not a web address.
    fun hostFromAddressBar(text: CharSequence?): String? {
        if (text.isNullOrBlank()) return null
        return normalizeDomain(text.toString())
    }

    // "reddit.com" -> "reddit", "news.bbc.co.uk" -> "bbc".
    fun coreName(domain: String): String {
        val parts = domain.split('.')
        if (parts.size < 2) return domain
        return if (parts.size >= 3 && parts[parts.size - 2] in SECOND_LEVEL) {
            parts[parts.size - 3]
        } else {
            parts[parts.size - 2]
        }
    }
}
