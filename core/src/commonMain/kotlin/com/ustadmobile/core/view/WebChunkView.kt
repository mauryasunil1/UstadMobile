package com.ustadmobile.core.view

import com.ustadmobile.core.impl.UmCallback
import com.ustadmobile.lib.db.entities.Container
import kotlin.js.JsName

interface WebChunkView : UstadView {

    @JsName("mountChunk")
    fun mountChunk(container: Container?, callback: UmCallback<String>)

    @JsName("loadUrl")
    fun loadUrl(url: String)

    @JsName("showError")
    fun showError(message: String)

    @JsName("setToolbarTitle")
    fun setToolbarTitle(title: String)

    @JsName("showErrorWithAction")
    fun showErrorWithAction(message: String, actionMessageId: Int, mimeType: String)

    companion object {

        const val VIEW_NAME = "WebChunk"
        const val ARG_CONTENT_ENTRY_ID = "entryId"
        const val ARG_CONTAINER_UID = "containerUid"
    }
}
