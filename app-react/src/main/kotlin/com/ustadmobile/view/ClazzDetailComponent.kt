package com.ustadmobile.view

import com.ccfraser.muirwik.components.MGridSize
import com.ccfraser.muirwik.components.MGridSpacing
import com.ustadmobile.core.controller.ClazzDetailPresenter
import com.ustadmobile.core.controller.UstadDetailPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.*
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.util.StyleManager
import com.ustadmobile.util.StyleManager.clazzDetailExtraInfo
import com.ustadmobile.util.StyleManager.contentContainer
import com.ustadmobile.util.StyleManager.defaultMarginTop
import com.ustadmobile.util.urlSearchParamsToMap
import com.ustadmobile.view.ext.umEntityAvatar
import com.ustadmobile.view.ext.umGridContainer
import com.ustadmobile.view.ext.umItem
import react.RBuilder
import react.RProps
import react.setState
import styled.css
import styled.styledDiv

class ClazzDetailComponent(mProps: RProps): UstadDetailComponent<Clazz>(mProps), ClazzDetailView {

    private var mPresenter: ClazzDetailPresenter? = null

    override val detailPresenter: UstadDetailPresenter<*, *>?
        get() = mPresenter

    override val viewName: String
        get() = ClazzDetailView.VIEW_NAME

    private var tabsToRender: List<UstadTab>? = null

    override var tabs: List<String>? = null
        set(value) {
            field = value
            tabsToRender = value?.map {
                val messageId = VIEWNAME_TO_TITLE_MAP[it.substringBefore("?",)] ?: 0
                UstadTab(
                    it.substringBefore("?"),
                    urlSearchParamsToMap(it.substring(it.lastIndexOf("?"))),
                    getString(messageId)
                )
            }?.toList()

        }

    override var entity: Clazz? = null
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override fun onCreate(arguments: Map<String, String>) {
        super.onCreate(arguments)
        mPresenter = ClazzDetailPresenter(this, arguments, this,di,this)
        mPresenter?.onCreate(mapOf())
    }

    override fun RBuilder.render() {
        styledDiv {
            css{
                +defaultMarginTop
                +contentContainer
            }
            umGridContainer(MGridSpacing.spacing6) {
                umItem(MGridSize.cells12, MGridSize.cells4){
                    umEntityAvatar(listItem = true, fallbackSrc = "assets/entry_placeholder.jpeg")
                }

                umItem(MGridSize.cells12, MGridSize.cells8){
                    styledDiv {
                        css {
                            +clazzDetailExtraInfo
                        }

                        tabsToRender?.let {
                            renderTabs(it)
                        }
                    }
                }
            }
        }
    }

    companion object {

        val VIEWNAME_TO_TITLE_MAP = mapOf(
            ClazzDetailOverviewView.VIEW_NAME to MessageID.overview,
            ContentEntryList2View.VIEW_NAME to MessageID.content,
            ClazzMemberListView.VIEW_NAME to MessageID.members,
            ClazzLogListAttendanceView.VIEW_NAME to MessageID.attendance,
            ClazzAssignmentListView.VIEW_NAME to MessageID.assignments
        )

    }
}