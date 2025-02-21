package com.ustadmobile.libcache.headers

class HttpHeadersImpl(
    internal val headers: List<HttpHeader>
) : HttpHeaders {

    override fun get(name: String): String? {
        return headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
    }

    override fun getAllByName(name: String): List<String> {
        return headers.filter { it.name.equals(name, ignoreCase = true) }.map { it.value }
    }

    override fun names(): Set<String> {
        return headers.map { it.name }.toSet()
    }



}