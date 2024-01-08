package com.ustadmobile.libcache

import com.ustadmobile.libcache.request.HttpRequest
import com.ustadmobile.libcache.response.HttpResponse

data class StoreResult(
    val urlKey: String,
    val request: HttpRequest,
    val response: HttpResponse,
    val integrity: String,
    val lockId: Int = 0,
)
