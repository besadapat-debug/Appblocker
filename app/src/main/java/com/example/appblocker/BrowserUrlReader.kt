package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

// Reads the address bar of common browsers through the accessibility
// service, so website blocking works without a VPN.
object BrowserUrlReader {

    private val CHROMIUM_IDS = listOf("url_bar")
    private val FIREFOX_IDS = listOf("mozac_browser_toolbar_url_view", "url_bar_title")

    private val ADDRESS_BAR_IDS: Map<String, List<String>> = mapOf(
        "com.android.chrome" to CHROMIUM_IDS,
        "com.chrome.beta" to CHROMIUM_IDS,
        "com.chrome.dev" to CHROMIUM_IDS,
        "com.chrome.canary" to CHROMIUM_IDS,
        "com.brave.browser" to CHROMIUM_IDS,
        "com.brave.browser_beta" to CHROMIUM_IDS,
        "com.microsoft.emmx" to CHROMIUM_IDS,
        "com.vivaldi.browser" to CHROMIUM_IDS,
        "com.kiwibrowser.browser" to CHROMIUM_IDS,
        "org.chromium.chrome" to CHROMIUM_IDS,
        "org.mozilla.firefox" to FIREFOX_IDS,
        "org.mozilla.firefox_beta" to FIREFOX_IDS,
        "org.mozilla.fenix" to FIREFOX_IDS,
        "org.mozilla.focus" to FIREFOX_IDS + "display_url",
        "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text", "custom_tab_toolbar_url_bar_text"),
        "com.sec.android.app.sbrowser.beta" to listOf("location_bar_edit_text", "custom_tab_toolbar_url_bar_text"),
        "com.opera.browser" to listOf("url_field"),
        "com.opera.mini.native" to listOf("url_field"),
        "com.opera.gx" to listOf("url_field"),
        "com.duckduckgo.mobile.android" to listOf("omnibarTextInput"),
        "com.mi.globalbrowser" to listOf("url"),
        "com.yandex.browser" to listOf("bro_omnibar_address_title_text")
    )

    fun isSupportedBrowser(packageName: String): Boolean = packageName in ADDRESS_BAR_IDS

    // Any app that is fundamentally a web browser. We can read the address bar
    // of the supported ones; the rest we can't, so the porn blocker can choose
    // to block them outright. Detected via the standard browsable web intent
    // plus a fallback name check.
    fun isKnownBrowserPackage(packageName: String): Boolean =
        packageName in ADDRESS_BAR_IDS || packageName in OTHER_BROWSERS

    private val OTHER_BROWSERS = setOf(
        "com.UCMobile.intl", "com.uc.browser.en", "com.apple.android.web",
        "com.aloha.browser", "com.jio.web", "phoenix.com.phoenix",
        "org.torproject.torbrowser", "com.tenta.android", "mark.via.gp",
        "acr.browser.lightning", "com.stoutner.privacybrowser.standard",
        "org.adblockplus.browser", "com.brave.browser_nightly",
        "com.cloudmosa.puffinFree", "com.opera.touch", "net.dezor.browser",
        "com.hsv.freeadblockerbrowser", "com.ksmobile.cb", "com.cootek.smartbrowser"
    )


    // Returns the raw address bar text if it contains a web address
    // (e.g. "youtube.com/shorts/abc"), or null if it can't be read right now.
    fun readAddress(service: AccessibilityService, packageName: String): String? {
        val ids = ADDRESS_BAR_IDS[packageName] ?: return null
        for (root in candidateRoots(service, packageName)) {
            for (id in ids) {
                val nodes = try {
                    root.findAccessibilityNodeInfosByViewId("$packageName:id/$id")
                } catch (e: Exception) {
                    emptyList<AccessibilityNodeInfo>()
                }
                for (node in nodes) {
                    val text = node.text?.toString()
                    if (WebsiteUtils.hostFromAddressBar(text) != null) return text
                }
            }
        }
        return null
    }

    private fun candidateRoots(service: AccessibilityService, packageName: String): List<AccessibilityNodeInfo> {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        service.rootInActiveWindow?.let { if (it.packageName == packageName) roots.add(it) }
        // When our block screen is on top it becomes the "active" window,
        // so also look through the other windows for the browser.
        try {
            service.windows.mapNotNull { it.root }
                .filter { it.packageName == packageName }
                .forEach { roots.add(it) }
        } catch (e: Exception) {
            // windows can throw if the service is being torn down
        }
        return roots
    }
}
