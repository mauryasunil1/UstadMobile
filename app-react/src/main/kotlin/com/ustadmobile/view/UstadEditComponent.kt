package com.ustadmobile.view

import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.UstadEditView
import com.ustadmobile.core.view.UstadEditView.Companion.ARG_ENTITY_JSON
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.util.RouteManager.getArgs
import org.w3c.dom.events.Event
import react.RProps
import react.RState

abstract class UstadEditComponent<T: Any>(mProps: RProps): UstadBaseComponent<RProps, RState>(mProps), UstadEditView<T> {

    override val viewName: String?
        get() = null

    abstract protected val mEditPresenter : UstadEditPresenter<*, T>?

    override var fieldsEnabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun finishWithResult(result: List<T>) {
        TODO("Not yet implemented")
    }


    override fun onComponentReady() {
        fabState = fabState.copy(visible = true, icon = "check",
            label = systemImpl.getString(MessageID.save, this))
    }

    override fun onFabClick(event: Event) {
        super.onFabClick(event)
        val entityVal = entity ?: return
        mEditPresenter?.handleClickSave(entityVal)
    }

    protected fun setEditTitle(newTitleId: Int, editStringId: Int) {
        val entityUid = getArgs()[ARG_ENTITY_UID]?.toLong() ?: 0L
        val entityJsonStr = getArgs()[ARG_ENTITY_JSON]
        title = if(entityUid != 0L || entityJsonStr != null){
           systemImpl.getString(editStringId, this)
        }else {
           systemImpl.getString(newTitleId, this)
        }
    }
}