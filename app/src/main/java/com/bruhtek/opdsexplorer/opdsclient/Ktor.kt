package com.bruhtek.opdsexplorer.opdsclient

import com.bruhtek.opdsexplorer.proto.AuthTypeProto
import com.bruhtek.opdsexplorer.proto.FeedProto
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.defaultSharedFormatCache


fun getClient(feed: FeedProto): HttpClient {
    val authType = feed.authType
    val username = feed.username
    val pass = feed.password

    if (authType == AuthTypeProto.NONE) {
        return basicClient
    }
    if (authType == AuthTypeProto.BASIC) {
        if (username.isNullOrEmpty() || pass.isNullOrEmpty()) {
            throw IllegalArgumentException("Username and password must be provided for BASIC authentication")
        }

        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                @OptIn(ExperimentalXmlUtilApi::class)
                val xml = XML {
                    policy = DefaultXmlSerializationPolicy(formatCache = defaultSharedFormatCache()) {
                        unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList()}
                    }
                }

                xml(xml)
                xml(xml, contentType = ContentType.Application.Atom)
            }
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials { BasicAuthCredentials(username = username, password = pass) }
                }
            }
        }
        return client
    }

    throw IllegalArgumentException("Unsupported authentication type: $authType")
}

private val basicClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        @OptIn(ExperimentalXmlUtilApi::class)
        val xml = XML {
            policy = DefaultXmlSerializationPolicy(formatCache = defaultSharedFormatCache()) {
                unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList()}
            }
        }

        xml(xml)
        xml(xml, contentType = ContentType.Application.Atom)
    }
}