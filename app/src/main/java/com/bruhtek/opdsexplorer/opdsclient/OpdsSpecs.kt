package com.bruhtek.opdsexplorer.opdsclient

import com.bruhtek.opdsexplorer.utils.getHostUrl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("entry", namespace = "http://www.w3.org/2005/Atom", prefix = "atom")
data class OpdsEntry(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val id: String,
    @XmlElement(true)
    val updated: String? = null,
    @XmlElement(true)
    val author: List<OpdsAuthor> = emptyList(),
    @XmlElement(true)
    val link: List<OpdsLink> = emptyList(),
    @XmlElement(true)
    val category: List<OpdsCategory> = emptyList(),
    @XmlElement(true)
    val summary: String? = null,
    @XmlElement(true)
    val content: ContentElement? = null,
    @XmlElement(true)
    @XmlSerialName("language", namespace = "http://purl.org/dc/terms/", prefix = "dc")
    val language: String? = null,
    @XmlElement(true)
    @XmlSerialName("issued", namespace = "http://purl.org/dc/terms/", prefix = "dc")
    val issued: String? = null,
) {
    fun thumbnailUrl(baseUrl: String): String {
        val url = link.firstOrNull { it.rel == "http://opds-spec.org/image/thumbnail" }
            ?: link.firstOrNull { it.rel == "http://opds-spec.org/image" }
            ?: link.firstOrNull { it.rel == "http://opds-spec.org/image/cover" }

        if (url?.href.isNullOrEmpty()) {
            return ""
        }

        if (url.href.startsWith("/")) {
            return getHostUrl(baseUrl) + url.href
        }

        return url.href
    }

    fun linkUrl(): String? {
        val url =
            link.firstOrNull { it.rel == "subsection" || it.type == "application/atom+xml;profile=opds-catalog" }
        return url?.href
    }

    fun subtitle(): String {
        var subtitle = summary ?: ""
        if (subtitle.isEmpty()) {
            if (content?.htmlContent?.contentString?.startsWith("<div") == true) {
                subtitle = author.joinToString(", ", transform = { it.name })
            } else {
                subtitle = content?.htmlContent?.contentString ?: author.joinToString(
                    ", ",
                    transform = { it.name })
            }
        }
        if (subtitle.isEmpty()) {
            return ""
        }
        if (subtitle.length > 50) {
            return subtitle.substring(0, 50) + "..."
        }
        return subtitle
    }
}

@Serializable
@XmlSerialName("content")
data class ContentElement(
    val type: String? = null,
    @XmlValue(true)
    val htmlContent: CompactFragment? = null
)


@Serializable
@XmlSerialName("author", namespace = "http://www.w3.org/2005/Atom")
data class OpdsAuthor(
    @XmlElement(true)
    val name: String,
    @XmlElement(true)
    val uri: String? = null,

    @XmlOtherAttributes()
    val others: Map<String, String> = emptyMap()
)

@Serializable
@XmlSerialName("link", namespace = "http://www.w3.org/2005/Atom")
data class OpdsLink(
    @XmlElement(false)
    val rel: String? = null,
    @XmlElement(false)
    val href: String,
    @XmlElement(false)
    val type: String? = null,
    @XmlElement(false)
    val title: String? = null,
    @XmlElement(true)
    val price: List<OpdsPrice>? = null
)

@Serializable
@XmlSerialName("price", namespace = "http://opds-spec.org/2010/catalog", prefix = "opds")
data class OpdsPrice(
    @XmlElement(false)
    val currencycode: String,
    @XmlValue(true)
    val amount: Double
)

@Serializable
@XmlSerialName("category", namespace = "http://www.w3.org/2005/Atom")
data class OpdsCategory(
    @XmlElement(false)
    val scheme: String? = null,
    @XmlElement(false)
    val term: String,
    @XmlElement(false)
    val label: String? = null
)

@Serializable
@XmlSerialName("feed", namespace = "http://www.w3.org/2005/Atom", prefix = "atom")
data class OpdsFeed(
    @XmlElement(true)
    val id: String,
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val subtitle: String? = null,
    @XmlElement(true)
    val updated: String? = null,
    @XmlElement(true)
    val author: List<OpdsAuthor> = emptyList(),
    @XmlElement(true)
    val link: List<OpdsLink> = emptyList(),
    @XmlElement(true)
    val entry: List<OpdsEntry> = emptyList(),
    @XmlElement(true)
    val icon: String? = null,
)

