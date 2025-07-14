package com.bruhtek.opdsexplorer.utils

import io.ktor.http.Url

fun getHostUrl(url: String): String {
    val parsed = Url(url);
    return "${parsed.protocol.name}://${parsed.host}${if (parsed.port != -1 && parsed.port != parsed.protocol.defaultPort) ":${parsed.port}" else ""}"
}