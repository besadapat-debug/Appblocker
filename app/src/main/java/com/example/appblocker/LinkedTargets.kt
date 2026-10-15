package com.example.appblocker

// Known app <-> website pairs, so blocking one can offer to block the other
// (block the Instagram app -> "also block instagram.com?", and vice versa).
object LinkedTargets {

    private data class Link(val packages: List<String>, val domains: List<String>)

    private val LINKS = listOf(
        Link(listOf("com.instagram.android", "com.instagram.lite"), listOf("instagram.com")),
        Link(listOf("com.facebook.katana", "com.facebook.lite"), listOf("facebook.com", "fb.com")),
        Link(listOf("com.facebook.orca"), listOf("messenger.com")),
        Link(listOf("com.instagram.barcelona"), listOf("threads.net", "threads.com")),
        Link(listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"), listOf("tiktok.com")),
        Link(listOf("com.google.android.youtube", "com.google.android.apps.youtube.music"), listOf("youtube.com", "youtu.be")),
        Link(listOf("com.twitter.android"), listOf("x.com", "twitter.com")),
        Link(listOf("com.snapchat.android"), listOf("snapchat.com")),
        Link(listOf("com.reddit.frontpage"), listOf("reddit.com")),
        Link(listOf("com.pinterest"), listOf("pinterest.com")),
        Link(listOf("com.linkedin.android"), listOf("linkedin.com")),
        Link(listOf("com.tumblr"), listOf("tumblr.com")),
        Link(listOf("xyz.blueskyweb.app"), listOf("bsky.app")),
        Link(listOf("com.bereal.ft"), listOf("bereal.com")),
        Link(listOf("com.whatsapp", "com.whatsapp.w4b"), listOf("whatsapp.com")),
        Link(listOf("org.telegram.messenger"), listOf("telegram.org")),
        Link(listOf("com.discord"), listOf("discord.com")),
        Link(listOf("tv.twitch.android.app"), listOf("twitch.tv")),
        Link(listOf("com.kick.mobile"), listOf("kick.com")),
        Link(listOf("com.netflix.mediaclient"), listOf("netflix.com")),
        Link(listOf("com.disney.disneyplus"), listOf("disneyplus.com")),
        Link(listOf("com.amazon.avod.thirdpartyclient"), listOf("primevideo.com")),
        Link(listOf("com.spotify.music"), listOf("spotify.com")),
        Link(listOf("com.amazon.mShop.android.shopping"), listOf("amazon.com", "amazon.com.au", "amazon.co.uk")),
        Link(listOf("com.ebay.mobile"), listOf("ebay.com", "ebay.com.au", "ebay.co.uk")),
        Link(listOf("com.einnovation.temu"), listOf("temu.com")),
        Link(listOf("com.zzkko"), listOf("shein.com")),
        Link(listOf("com.tinder"), listOf("tinder.com")),
        Link(listOf("co.hinge.app"), listOf("hinge.co")),
        Link(listOf("com.bumble.app"), listOf("bumble.com")),
        Link(listOf("com.roblox.client"), listOf("roblox.com")),
        Link(listOf("com.ninegag.android.app"), listOf("9gag.com")),
        Link(listOf("com.quora.android"), listOf("quora.com"))
    )

    fun domainsForApp(packageName: String): List<String> =
        LINKS.filter { packageName in it.packages }.flatMap { it.domains }

    // Packages known to belong to this website (caller filters to installed ones).
    fun packagesForDomain(domain: String): List<String> =
        LINKS.filter { link ->
            link.domains.any { domain == it || domain.endsWith(".$it") }
        }.flatMap { it.packages }
}
