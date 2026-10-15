package com.example.appblocker

import android.content.Context
import java.util.zip.GZIPInputStream

// Bundled list of ~62k adult domains (assets/adult_domains.txt.gz), loaded
// once into a hash set. A host is blocked if it or any parent domain is in
// the set, so "de.pornhub.com" matches "pornhub.com".
object AdultBlocklist {

    @Volatile
    private var domains: Set<String>? = null

    // Extra safety net for sites not on the list: any host or URL containing
    // one of these very-adult words is treated as adult.
    private val KEYWORDS = listOf(
        "porn", "xxx", "xvideos", "xhamster", "xnxx", "brazzers", "onlyfans",
        "rule34", "hentai", "nsfw", "camgirl", "sexcam", "escort", "fetish",
        "redtube", "youporn", "spankbang", "nhentai", "e-hentai", "fapello"
    )

    fun ensureLoaded(context: Context) {
        if (domains != null) return
        synchronized(this) {
            if (domains != null) return
            domains = load(context)
        }
    }

    fun isAdultHost(context: Context, host: String): Boolean {
        ensureLoaded(context)
        val set = domains ?: return false
        var h = host
        while (true) {
            if (h in set) return true
            val dot = h.indexOf('.')
            if (dot < 0) return false
            h = h.substring(dot + 1)
            if (h.count { it == '.' } == 0) return false
        }
    }

    fun matchesKeyword(host: String, addressText: String): Boolean {
        val hay = (host + " " + addressText).lowercase()
        return KEYWORDS.any { hay.contains(it) }
    }

    // Returns the reason string if blocked, else null.
    fun blockReason(context: Context, host: String, addressText: String): String? = when {
        isAdultHost(context, host) -> host
        matchesKeyword(host, addressText) -> "adult content"
        else -> null
    }

    private fun load(context: Context): Set<String> = try {
        GZIPInputStream(context.assets.open("adult_domains.txt.gz")).bufferedReader().use { reader ->
            val set = HashSet<String>(70000)
            reader.forEachLine { line ->
                val d = line.trim()
                if (d.isNotEmpty() && d[0] != '#') set.add(d)
            }
            set
        }
    } catch (e: Exception) {
        emptySet()
    }
}
